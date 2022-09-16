/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.demo.control.service.ticketing.procedure

import org.calypsonet.keyple.demo.common.parser.*
import org.calypsonet.keyple.demo.common.parser.model.CardContract
import org.calypsonet.keyple.demo.common.parser.model.CardEvent
import org.calypsonet.keyple.demo.common.parser.model.constant.ContractPriority
import org.calypsonet.keyple.demo.common.parser.model.constant.VersionNumber
import org.calypsonet.keyple.demo.control.ApplicationSettings
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.RECORD_NUMBER_1
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.RECORD_NUMBER_4
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.SFI_CONTRACTS
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.SFI_COUNTER
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.SFI_ENVIRONMENT_AND_HOLDER
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.SFI_EVENTS_LOG
import org.calypsonet.keyple.demo.control.service.ticketing.TicketingService
import org.calypsonet.keyple.demo.control.service.ticketing.exception.ControlException
import org.calypsonet.keyple.demo.control.service.ticketing.exception.EnvironmentControlException
import org.calypsonet.keyple.demo.control.service.ticketing.exception.EnvironmentControlExceptionKey
import org.calypsonet.keyple.demo.control.service.ticketing.exception.EventControlException
import org.calypsonet.keyple.demo.control.service.ticketing.exception.EventControlExceptionKey
import org.calypsonet.keyple.demo.control.service.ticketing.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.service.ticketing.model.Contract
import org.calypsonet.keyple.demo.control.service.ticketing.model.Location
import org.calypsonet.keyple.demo.control.service.ticketing.model.Status
import org.calypsonet.keyple.demo.control.service.ticketing.model.Validation
import org.calypsonet.keyple.demo.control.service.ticketing.model.mapper.ContractMapper
import org.calypsonet.keyple.demo.control.service.ticketing.model.mapper.ValidationMapper
import org.calypsonet.terminal.calypso.WriteAccessLevel
import org.calypsonet.terminal.calypso.card.CalypsoCard
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.joda.time.DateTime
import timber.log.Timber

class ControlProcedure {

  fun launch(
      now: DateTime,
      calypsoCard: CalypsoCard,
      isSecureSessionMode: Boolean,
      ticketingService: TicketingService,
      locations: List<Location>
  ): CardReaderResponse {

    val cardReader = ticketingService.getCardReader()

    val errorMessage: String?
    var errorTitle: String? = null
    var validation: Validation? = null
    var status: Status = Status.ERROR

    val calypsoExtensionService = CalypsoExtensionService.getInstance()

    try {
      val cardTransaction =
          try {
            if (isSecureSessionMode) {
              // The transaction will be certified by the SAM in a secure session.
              calypsoExtensionService.createCardTransaction(
                  cardReader, calypsoCard, ticketingService.getSecuritySettings())
            } else {
              // The transaction will take place without being certified by a SAM
              calypsoExtensionService.createCardTransactionWithoutSecurity(cardReader, calypsoCard)
            }
          } catch (e: Exception) {
            // TODO check which condition could lead here
            Timber.w(e)
            calypsoExtensionService.createCardTransactionWithoutSecurity(cardReader, calypsoCard)
          }

      // Read and unpack environment structure from the binary present in the environment record.
      cardTransaction.prepareReadRecord(SFI_ENVIRONMENT_AND_HOLDER, RECORD_NUMBER_1)

      if (isSecureSessionMode) {
        // Open a secure session and read the Environment file
        cardTransaction.processOpening(WriteAccessLevel.DEBIT)
      } else {
        // Read the Environment file
        cardTransaction.processCommands()
      }

      val efEnvironmentHolder = calypsoCard.getFileBySfi(SFI_ENVIRONMENT_AND_HOLDER)
      val env = CardEnvironmentHolderParser().parse(efEnvironmentHolder.data.content)

      // Checks if the EnvVersionNumber of the Environment structure is not the expected one (==1
      // for the current version), throws a dedicated exception if it is not the case (cancels the
      // secured session before if any).
      if (env.envVersionNumber != VersionNumber.CURRENT_VERSION.key) {
        if (isSecureSessionMode) {
          cardTransaction.processCancel()
        }
        throw EnvironmentControlException(EnvironmentControlExceptionKey.WRONG_VERSION_NUMBER)
      }

      // Checks if the EnvEndDate points to a date in the past, throws a dedicated exception if it
      // is the case (cancels the secured session before if any).
      val envEndDate = DateTime(env.getEnvEndDateAsDate())
      if (envEndDate.isBefore(now)) {
        if (isSecureSessionMode) {
          cardTransaction.processCancel()
        }
        throw EnvironmentControlException(EnvironmentControlExceptionKey.EXPIRED)
      }

      // Read and unpack the last event record.
      cardTransaction.prepareReadRecord(SFI_EVENTS_LOG, RECORD_NUMBER_1)
      cardTransaction.processCommands()

      val efEventLog = calypsoCard.getFileBySfi(SFI_EVENTS_LOG)
      val event = CardEventParser().parse(efEventLog.data.content)

      // Checks if EventVersionNumber is the expected one (==1 for the current version), throws a
      // dedicated exception if it is not the case (differentiates if it is a non-personalized card)
      // (cancels the secured session before if any).
      val eventVersionNumber = event.eventVersionNumber
      if (eventVersionNumber != VersionNumber.CURRENT_VERSION.key) {
        if (isSecureSessionMode) {
          cardTransaction.processCancel()
        }
        if (eventVersionNumber == VersionNumber.UNDEFINED.key) {
          throw EventControlException(EventControlExceptionKey.CLEAN_CARD)
        } else {
          throw EventControlException(EventControlExceptionKey.WRONG_VERSION_NUMBER)
        }
      }

      var contractEventNotValid = false
      val contractUsed = event.eventContractUsed

      val eventDateTime = DateTime(event.getEventDate())
      val eventValidityEndDate = eventDateTime.plusMinutes(ApplicationSettings.validationPeriod)

      // Checks contract event validity
      // Event location should match the location configured for this terminal
      if (ApplicationSettings.location.id != event.eventLocation) {
        contractEventNotValid = true
      }
      // Event date should be today
      else if (eventDateTime.withTimeAtStartOfDay().isBefore(now.withTimeAtStartOfDay())) {
        contractEventNotValid = true
      }
      // Event end of validity should be in the future
      else if (eventValidityEndDate.isBefore(now)) {
        contractEventNotValid = true
      }

      // Read all contracts and the counter file
      cardTransaction.prepareReadRecords(
          SFI_CONTRACTS, RECORD_NUMBER_1, RECORD_NUMBER_4, CONTRACT_RECORD_SIZE)
      cardTransaction.prepareReadCounter(SFI_COUNTER, COUNTER_RECORDS_NB)
      cardTransaction.processCommands()

      val efCounter = calypsoCard.getFileBySfi(SFI_COUNTER)
      val efContractParser = calypsoCard.getFileBySfi(SFI_CONTRACTS)
      val contracts = mutableMapOf<Int, CardContract>()

      // Unpack all contracts
      efContractParser.data.allRecordsContent.forEach {
        contracts[it.key] = CardContractParser().parse(it.value)
      }

      // Retrieve contract used for last event
      val eventContract =
          contracts.toList().filter { it.first == event.eventContractUsed }.map { it.second }

      if (isValidEvent(event)) {
        validation =
            if (eventContract.isNotEmpty()) {
              ValidationMapper.map(
                  event = event, contract = eventContract[0], locations = locations)
            } else {
              ValidationMapper.map(event = event, contract = null, locations = locations)
            }
      }

      val displayedContract = arrayListOf<Contract>()
      contracts.forEach {
        val record = it.key
        val contract = it.value
        var contractExpired = false
        var contractValidated = false

        if (contract.contractVersionNumber == VersionNumber.UNDEFINED) {
          // If the ContractVersionNumber == 0 then the contract is blank, move on to the next
          // contract.
        } else if (contract.contractVersionNumber != VersionNumber.CURRENT_VERSION) {
          // TODO Check what to do here
        } else {
          // Check th contractAuthenticator
          @Suppress("ControlFlowWithEmptyBody")
          if (isSecureSessionMode && contract.contractAuthenticator != 0) {
            // Place here the PSO Verify Signature command of the SAM to check the
            // contractAuthenticator
          }
          // Check ContractValidityEndDate points to a date in the past mark contract as expired.
          val contractValidityEndDate = DateTime(contract.getContractValidityEndDateAsDate())
          if (contractValidityEndDate.isBefore(now)) {
            contractExpired = true
          }

          // If EventContractUsed points to the current contract index & not valid flag is false
          // then mark it as validated.
          if (contractUsed == record && !contractEventNotValid) {
            contractValidated = true
          }

          var validationDate: DateTime? = null
          if (contractValidated && contractUsed == record) {
            validationDate = eventDateTime
          }

          // If the ContractTariff value for the contract is 2 or 3, unpack the counter associated
          // to the contract to extract the counter value.
          val nbTicketsLeft =
              if (contract.contractTariff == ContractPriority.MULTI_TRIP) {
                efCounter.data.getContentAsCounterValue(record)
              } else {
                null
              }

          // Add contract data to the list of contracts read to return to the upper layer.
          displayedContract.add(
              ContractMapper.map(
                  contract = contract,
                  record = record,
                  contractExpired = contractExpired,
                  contractValidated = contractValidated,
                  validationDate = validationDate,
                  nbTicketsLeft = nbTicketsLeft))
        }
      }

      Timber.i("Control procedure result : STATUS_OK")
      status = Status.TICKETS_FOUND

      // Close the secure session if any
      if (isSecureSessionMode) {
        if (status == Status.TICKETS_FOUND) {
          cardTransaction.processClosing()
        } else {
          cardTransaction.processCancel()
        }
      }

      var validationList: ArrayList<Validation>? = null
      if (validation != null) {
        validationList = arrayListOf(validation)
      }

      // Return the status of the operation to the upper layer. <Exit process>
      return CardReaderResponse(
          status = status, lastValidationsList = validationList, titlesList = displayedContract)
    } catch (e: Exception) {
      errorMessage = e.message
      Timber.e(e)
      when (e) {
        is EnvironmentControlException -> {}
        is EventControlException -> {
          status =
              if (e.key == EventControlExceptionKey.CLEAN_CARD) {
                Status.EMPTY_CARD
              } else {
                Status.ERROR
              }
        }
        is ControlException -> {
          errorTitle = e.title
          status = Status.ERROR
        }
        else -> {
          status = Status.ERROR
        }
      }
    }

    return CardReaderResponse(
        status = status,
        titlesList = arrayListOf(),
        errorTitle = errorTitle,
        errorMessage = errorMessage)
  }

  /**
   * An event is considered valid for display if an eventTimeStamp or an eventDateStamp has been set
   * during a previous validation
   */
  private fun isValidEvent(event: CardEvent): Boolean {
    return event.eventTimeStamp != 0 || event.eventDateStamp != 0
  }

  companion object {
    const val COUNTER_RECORDS_NB = 4
    const val CONTRACT_RECORD_SIZE = 29
  }
}
