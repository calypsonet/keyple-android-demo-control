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
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting
import org.calypsonet.terminal.reader.CardReader
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.joda.time.DateTime
import timber.log.Timber

class ControlProcedure {

  fun launch(
      now: DateTime,
      cardReader: CardReader,
      calypsoCard: CalypsoCard,
      cardSecuritySettings: CardSecuritySetting?,
      locations: List<Location>
  ): CardReaderResponse {

    val errorMessage: String?
    var errorTitle: String? = null
    var validation: Validation? = null
    var status: Status = Status.ERROR
    val isSecureSessionMode = cardSecuritySettings != null
    val calypsoExtensionService = CalypsoExtensionService.getInstance()

    try {
      val cardTransaction =
          try {
            if (isSecureSessionMode) {
              // Step 1.1
              // The transaction will be certified by the SAM in a secure session.
              calypsoExtensionService.createCardTransaction(
                  cardReader, calypsoCard, cardSecuritySettings)
            } else {
              // Step 1.2
              // The transaction will take place without being certified by a SAM
              calypsoExtensionService.createCardTransactionWithoutSecurity(cardReader, calypsoCard)
            }
          } catch (e: Exception) {
            // TODO check which condition could lead here
            Timber.w(e)
            calypsoExtensionService.createCardTransactionWithoutSecurity(cardReader, calypsoCard)
          }

      // Step 2 - Read and unpack environment structure from the binary present in the environment
      // record.
      cardTransaction.prepareReadRecord(SFI_ENVIRONMENT_AND_HOLDER, RECORD_NUMBER_1)

      if (isSecureSessionMode) {
        // Open a transaction to read/write the Calypso Card and read the Environment file
        cardTransaction.processOpening(WriteAccessLevel.DEBIT)
      } else {
        // Just read the Environment file
        cardTransaction.processCommands()
      }

      val efEnvironmentHolder = calypsoCard.getFileBySfi(SFI_ENVIRONMENT_AND_HOLDER)
      val env = CardEnvironmentHolderParser().parse(efEnvironmentHolder.data.content)

      // Step 3 - If EnvVersionNumber of the Environment structure is not the expected one (==1 for
      // the current version) reject the card.
      // <Abort Secure Session if any>
      if (env.envVersionNumber != VersionNumber.CURRENT_VERSION.key) {
        if (isSecureSessionMode) {
          cardTransaction.processCancel()
        }
        throw EnvironmentControlException(EnvironmentControlExceptionKey.WRONG_VERSION_NUMBER)
      }

      // Step 4 - If EnvEndDate points to a date in the past reject the card.
      // <Abort Secure Session if any>
      val envEndDate = DateTime(env.getEnvEndDateAsDate())
      if (envEndDate.isBefore(now)) {
        if (isSecureSessionMode) {
          cardTransaction.processCancel()
        }
        throw EnvironmentControlException(EnvironmentControlExceptionKey.EXPIRED)
      }

      // Step 5 - Read and unpack the last event record.
      cardTransaction.prepareReadRecord(SFI_EVENTS_LOG, RECORD_NUMBER_1)
      cardTransaction.processCommands()

      val efEventLog = calypsoCard.getFileBySfi(SFI_EVENTS_LOG)
      val event = CardEventParser().parse(efEventLog.data.content)

      // Step 6 - If EventVersionNumber is not the expected one (==1 for the current version) reject
      // the card (if ==0 return error status indicating clean card).
      // <Abort Secure Session if any>
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

      var contractEventValid = true
      val contractUsed = event.eventContractUsed

      val eventDateTime = DateTime(event.getEventDate())
      val eventValidityEndDate = eventDateTime.plusMinutes(ApplicationSettings.validationPeriod)

      // Step 7 - If EventLocation != value configured in the control terminal set the validated
      // contract valid flag as false and go to point CNT_READ.
      if (ApplicationSettings.location.id != event.eventLocation) {
        contractEventValid = false
      }
      // Step 8 - Else If EventDateStamp points to a date in the past
      // -> set the validated contract valid flag as false and go to point CNT_READ.
      else if (eventDateTime.withTimeAtStartOfDay().isBefore(now.withTimeAtStartOfDay())) {
        contractEventValid = false
      }

      // Step 9 - Else If (EventTimeStamp + Validation period configure in the control terminal) <
      // current time of the control terminal
      //  -> set the validated contract valid flag as false.
      else if (eventValidityEndDate.isBefore(now)) {
        contractEventValid = false
      }

      // Step 10 - CNT_READ: Read all contracts and the counter file
      cardTransaction.prepareReadRecords(
          SFI_CONTRACTS, RECORD_NUMBER_1, RECORD_NUMBER_4, CONTRACT_RECORD_SIZE)
      cardTransaction.prepareReadCounter(SFI_COUNTER, COUNTER_RECORDS_NB)
      cardTransaction.processCommands()

      val efCounters = calypsoCard.getFileBySfi(SFI_COUNTER)

      val efContracts = calypsoCard.getFileBySfi(SFI_CONTRACTS)
      val contracts = mutableMapOf<Int, CardContract>()

      // Step 11 - For each contract:
      efContracts.data.allRecordsContent.forEach {
        // Step 12 - Unpack the contract
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
          // Step 13 - If the ContractVersionNumber == 0 then the contract is blank, move on to the
          // next contract.
        } else if (contract.contractVersionNumber != VersionNumber.CURRENT_VERSION) {
          // Step 14 - If ContractVersionNumber is not the expected one (==1 for the current
          // version) reject the card.
          // <Abort Secure Session if any>
        } else {
          // Step 15 - If SAM available and ContractAuthenticator is not 0 perform the verification
          // of the value
          // by using the PSO Verify Signature command of the SAM.
          @Suppress("ControlFlowWithEmptyBody")
          if (isSecureSessionMode && contract.contractAuthenticator != 0) {
            // Step 15.1 - If the value is wrong reject the card.
            // <Abort Secure Session if any>
            // Step 15.2 - If the value of ContractSaleSam is present in the SAM Black List reject
            // the card.
            // <Abort Secure Session if any>
            // TODO: steps 15.1 & 15.2
          }
          // Step 16 - If ContractValidityEndDate points to a date in the past mark contract as
          // expired.
          val contractValidityEndDate = DateTime(contract.getContractValidityEndDateAsDate())
          if (contractValidityEndDate.isBefore(now)) {
            contractExpired = true
          }

          // Step 17 - If EventContractUsed points to the current contract index
          // & not valid flag is false then mark it as Validated.
          if (contractUsed == record && contractEventValid) {
            contractValidated = true
          }

          var validationDate: DateTime? = null
          if (contractValidated && contractUsed == record) {
            validationDate = eventDateTime
          }

          // Step 18 -   If the ContractTariff value for the contract is 2 or 3, unpack the counter
          // associated to the contract to extract the counter value.
          val nbTicketsLeft =
              if (contract.contractTariff == ContractPriority.MULTI_TRIP) {
                efCounters.data.getContentAsCounterValue(record)
              } else {
                null
              }

          // Step 19 - Add contract data to the list of contracts read to return to the upper layer.
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

      // Step 20 - If isSecureSessionMode is true, Close the session
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

      // Step 21 - Return the status of the operation to the upper layer. <Exit process>
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
