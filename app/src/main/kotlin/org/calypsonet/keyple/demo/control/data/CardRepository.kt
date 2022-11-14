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
package org.calypsonet.keyple.demo.control.data

import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.constant.CardConstant
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.common.parser.*
import org.calypsonet.keyple.demo.control.data.model.AppSettings
import org.calypsonet.keyple.demo.control.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.data.model.Contract
import org.calypsonet.keyple.demo.control.data.model.Location
import org.calypsonet.keyple.demo.control.data.model.Status
import org.calypsonet.keyple.demo.control.data.model.Validation
import org.calypsonet.keyple.demo.control.data.model.mapper.ContractMapper
import org.calypsonet.keyple.demo.control.data.model.mapper.ValidationMapper
import org.calypsonet.terminal.calypso.WriteAccessLevel
import org.calypsonet.terminal.calypso.card.CalypsoCard
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting
import org.calypsonet.terminal.reader.CardReader
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import timber.log.Timber

class CardRepository {

  fun executeControlProcedure(
      controlDateTime: LocalDateTime,
      cardReader: CardReader,
      calypsoCard: CalypsoCard,
      cardSecuritySettings: CardSecuritySetting?,
      locations: List<Location>
  ): CardReaderResponse {

    var errorMessage: String?
    val errorTitle: String? = null
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
      cardTransaction.prepareReadRecord(CardConstant.SFI_ENVIRONMENT_AND_HOLDER, 1)

      if (isSecureSessionMode) {
        // Open a transaction to read/write the Calypso Card and read the Environment file
        cardTransaction.processOpening(WriteAccessLevel.DEBIT)
      } else {
        // Just read the Environment file
        cardTransaction.processCommands()
      }

      val efEnvironmentHolder = calypsoCard.getFileBySfi(CardConstant.SFI_ENVIRONMENT_AND_HOLDER)
      val env = EnvironmentHolderStructureParser().parse(efEnvironmentHolder.data.content)

      // Step 3 - If EnvVersionNumber of the Environment structure is not the expected one (==1 for
      // the current version) reject the card.
      // <Abort Secure Session if any>
      if (env.envVersionNumber != VersionNumber.CURRENT_VERSION) {
        if (isSecureSessionMode) {
          cardTransaction.processCancel()
        }
        throw EnvironmentException("wrong version number")
      }

      // Step 4 - If EnvEndDate points to a date in the past reject the card.
      // <Abort Secure Session if any>
      if (env.envEndDate.getDate().isBefore(controlDateTime.toLocalDate())) {
        if (isSecureSessionMode) {
          cardTransaction.processCancel()
        }
        throw EnvironmentException("End date expired")
      }

      // Step 5 - Read and unpack the last event record.
      cardTransaction.prepareReadRecord(CardConstant.SFI_EVENTS_LOG, 1)
      cardTransaction.processCommands()

      val efEventLog = calypsoCard.getFileBySfi(CardConstant.SFI_EVENTS_LOG)
      val event = EventStructureParser().parse(efEventLog.data.content)

      // Step 6 - If EventVersionNumber is not the expected one (==1 for the current version) reject
      // the card (if ==0 return error status indicating clean card).
      // <Abort Secure Session if any>
      val eventVersionNumber = event.eventVersionNumber
      if (eventVersionNumber != VersionNumber.CURRENT_VERSION) {
        if (isSecureSessionMode) {
          cardTransaction.processCancel()
        }
        if (eventVersionNumber == VersionNumber.UNDEFINED) {
          throw EventCleanCardException()
        } else {
          throw EventWrongVersionNumberException()
        }
      }

      var contractEventValid = true
      val contractUsed = event.eventContractUsed

      val eventValidityEndDate =
          event.eventDatetime.plusMinutes(AppSettings.validationPeriod.toLong())

      // Step 7 - If EventLocation != value configured in the control terminal set the validated
      // contract valid flag as false and go to point CNT_READ.
      if (AppSettings.location.id != event.eventLocation) {
        contractEventValid = false
      }
      // Step 8 - Else If EventDateStamp points to a date in the past
      // -> set the validated contract valid flag as false and go to point CNT_READ.
      else if (event.eventDatetime.isBefore(controlDateTime.toLocalDate().atStartOfDay())) {
        contractEventValid = false
      }

      // Step 9 - Else If (EventTimeStamp + Validation period configure in the control terminal) <
      // current time of the control terminal
      //  -> set the validated contract valid flag as false.
      else if (eventValidityEndDate.isBefore(controlDateTime)) {
        contractEventValid = false
      }

      val nbContractRecords =
          when (calypsoCard.productType) {
            CalypsoCard.ProductType.BASIC -> 1
            CalypsoCard.ProductType.LIGHT -> 2
            else -> 4
          }

      // Step 10 - CNT_READ: Read all contracts and the counter file
      cardTransaction.prepareReadRecords(
          CardConstant.SFI_CONTRACTS, 1, nbContractRecords, CardConstant.CONTRACT_RECORD_SIZE_BYTES)
      cardTransaction.prepareReadCounter(CardConstant.SFI_COUNTERS, nbContractRecords)
      cardTransaction.processCommands()

      val efCounters = calypsoCard.getFileBySfi(CardConstant.SFI_COUNTERS)

      val efContracts = calypsoCard.getFileBySfi(CardConstant.SFI_CONTRACTS)
      val contracts = mutableMapOf<Int, ContractStructure>()

      // Step 11 - For each contract:
      efContracts.data.allRecordsContent.forEach {
        // Step 12 - Unpack the contract
        contracts[it.key] = ContractStructureParser().parse(it.value)
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
          if (contract.contractValidityEndDate.getDate().isBefore(controlDateTime.toLocalDate())) {
            contractExpired = true
          }

          // Step 17 - If EventContractUsed points to the current contract index
          // & not valid flag is false then mark it as Validated.
          if (contractUsed == record && contractEventValid) {
            contractValidated = true
          }

          var validationDateTime: LocalDateTime? = null
          if (contractValidated && contractUsed == record) {
            validationDateTime = event.eventDatetime
          }

          // Step 18 -   If the ContractTariff value for the contract is 2 or 3, unpack the counter
          // associated to the contract to extract the counter value.
          val nbTicketsLeft =
              if (contract.contractTariff == PriorityCode.MULTI_TRIP) {
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
                  validationDateTime = validationDateTime,
                  nbTicketsLeft = nbTicketsLeft))
        }
      }

      Timber.i("Control procedure result: STATUS_OK")
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
        is EnvironmentException -> {
          errorMessage = "Environment error: $errorMessage"
        }
        is EventCleanCardException -> {
          status = Status.EMPTY_CARD
        }
        is EventWrongVersionNumberException -> {
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
  private fun isValidEvent(event: EventStructure): Boolean {
    return event.eventTimeStamp.value != 0 || event.eventDateStamp.value != 0
  }

  private class EnvironmentException(message: String) : RuntimeException(message)

  private class EventCleanCardException : RuntimeException("clean card")

  private class EventWrongVersionNumberException : RuntimeException("wrong version number")
}
