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
package org.calypsonet.keyple.demo.control.ticketing.procedure

import org.calypsonet.keyple.demo.common.parser.*
import org.calypsonet.keyple.demo.common.parser.model.CardContract
import org.calypsonet.keyple.demo.common.parser.model.CardEvent
import org.calypsonet.keyple.demo.common.parser.model.constant.ContractPriority
import org.calypsonet.keyple.demo.common.parser.model.constant.VersionNumber
import org.calypsonet.keyple.demo.control.exception.ControlException
import org.calypsonet.keyple.demo.control.exception.EnvironmentControlException
import org.calypsonet.keyple.demo.control.exception.EnvironmentControlExceptionKey
import org.calypsonet.keyple.demo.control.exception.EventControlException
import org.calypsonet.keyple.demo.control.exception.EventControlExceptionKey
import org.calypsonet.keyple.demo.control.exception.NoLocationDefinedException
import org.calypsonet.keyple.demo.control.models.CardReaderResponse
import org.calypsonet.keyple.demo.control.models.Contract
import org.calypsonet.keyple.demo.control.models.ControlAppSettings
import org.calypsonet.keyple.demo.control.models.Location
import org.calypsonet.keyple.demo.control.models.Status
import org.calypsonet.keyple.demo.control.models.Validation
import org.calypsonet.keyple.demo.control.models.mapper.ContractMapper
import org.calypsonet.keyple.demo.control.models.mapper.ValidationMapper
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_1
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_4
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SAM_PROFILE_NAME
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_Contracts
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_EnvironmentAndHolder
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_EventsLog
import org.calypsonet.keyple.demo.control.ticketing.TicketingSession
import org.calypsonet.terminal.calypso.WriteAccessLevel
import org.calypsonet.terminal.calypso.card.CalypsoCard
import org.calypsonet.terminal.reader.CardReader
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.joda.time.DateTime
import timber.log.Timber

class ControlProcedure {

  fun launch(
      now: DateTime,
      calypsoCard: CalypsoCard,
      samReader: CardReader?,
      ticketingSession: TicketingSession,
      locations: List<Location>
  ): CardReaderResponse {

    val cardReader = ticketingSession.getCardReader()

    val errorMessage: String?
    var errorTitle: String? = null
    var validation: Validation? = null
    var status: Status = Status.ERROR

    val calypsoCardExtensionProvider = CalypsoExtensionService.getInstance()

    try {
      var inTransactionFlag:
          Boolean // true if a SAM is available and a secure session have been opened
      val cardTransaction =
          try {
            if (samReader != null) {
              /*
               * Step 1.1 - If SAM available, Open a Validation session reading the environment record, set inTransactionFlag to true and go to point 2.
               */
              inTransactionFlag = true

              ticketingSession.setupCardResourceService(SAM_PROFILE_NAME)

              calypsoCardExtensionProvider.createCardTransaction(
                  cardReader, calypsoCard, ticketingSession.getSecuritySettings())
            } else {
              /*
               * Step 1.2 - Else, read the environment record.
               */
              inTransactionFlag = false
              calypsoCardExtensionProvider.createCardTransactionWithoutSecurity(
                  cardReader, calypsoCard)
            }
          } catch (e: IllegalStateException) {
            Timber.w(e)
            inTransactionFlag = false
            calypsoCardExtensionProvider.createCardTransactionWithoutSecurity(
                cardReader, calypsoCard)
          } catch (e: Exception) {
            Timber.w(e)
            inTransactionFlag = false
            calypsoCardExtensionProvider.createCardTransactionWithoutSecurity(
                cardReader, calypsoCard)
          }

      /*
       * Step 2 - Unpack environment structure from the binary present in the environment record.
       */
      cardTransaction.prepareReadRecord(SFI_EnvironmentAndHolder, RECORD_NUMBER_1.toInt())

      if (inTransactionFlag) {
        /*
         * Open a transaction to read/write the Calypso Card and read the Environment file
         */
        cardTransaction.processOpening(WriteAccessLevel.DEBIT)
      } else {
        /*
         * Read the Environment file
         */
        cardTransaction.processCommands()
      }

      val efEnvironmentHolder = calypsoCard.getFileBySfi(SFI_EnvironmentAndHolder)
      val env = CardEnvironmentHolderParser().parse(efEnvironmentHolder.data.content)

      /*
       * Step 3 - If EnvVersionNumber of the Environment structure is not the expected one (==1 for the current version) reject the card.
       * <Abort Transaction if inTransactionFlag is true and exit process>
       */
      if (env.envVersionNumber != VersionNumber.CURRENT_VERSION.key) {
        if (inTransactionFlag) {
          cardTransaction.processCancel()
        }
        throw EnvironmentControlException(EnvironmentControlExceptionKey.WRONG_VERSION_NUMBER)
      }

      /*
       * Step 4 - If EnvEndDate points to a date in the past reject the card.
       * <Abort Transaction if inTransactionFlag is true and exit process>
       */
      val envEndDate = DateTime(env.getEnvEndDateAsDate())
      if (envEndDate.isBefore(now)) {
        if (inTransactionFlag) {
          cardTransaction.processCancel()
        }
        throw EnvironmentControlException(EnvironmentControlExceptionKey.EXPIRED)
      }

      /*
       * Step 5 - Read and unpack the last event record.
       */
      cardTransaction.prepareReadRecord(SFI_EventsLog, RECORD_NUMBER_1.toInt())
      cardTransaction.processCommands()

      val efEventLog = calypsoCard.getFileBySfi(SFI_EventsLog)
      val event = CardEventParser().parse(efEventLog.data.content)

      /*
       * Step 6 - If EventVersionNumber is not the expected one (==1 for the current version) reject the card
       * (if ==0 return error status indicating clean card).
       * <Abort Transaction if inTransactionFlag is true and exit process>
       */
      val eventVersionNumber = event.eventVersionNumber
      if (eventVersionNumber != VersionNumber.CURRENT_VERSION.key) {
        if (inTransactionFlag) {
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
      val eventValidityEndDate = eventDateTime.plusMinutes(ControlAppSettings.validationPeriod ?: 0)

      /*
       * Step 7 - If EventLocation != value configured in the control terminal set the validated contract not valid flag as true and go to point CNT_READ.
       */
      if (ControlAppSettings.location == null) {
        throw NoLocationDefinedException()
      } else if (ControlAppSettings.location!!.id != event.eventLocation) {
        contractEventNotValid = true
      }
      /*
       * Step 8 - Else If EventDateStamp points to a date in the past
       * -> set the validated contract not valid flag as true and go to point CNT_READ.
       */
      else if (eventDateTime.withTimeAtStartOfDay().isBefore(now.withTimeAtStartOfDay())) {
        contractEventNotValid = true
      }

      /*
       * Step 9 - Else If (EventTimeStamp + Validation period configure in the control terminal) < current time of the control terminal
       *  -> set the validated contract not valid flag as true.
       */
      else if (eventValidityEndDate.isBefore(now)) {
        contractEventNotValid = true
      }

      /*
       * Step 10 - CNT_READ: Read all contracts and the counter file
       */
      cardTransaction.prepareReadRecords(
          SFI_Contracts, RECORD_NUMBER_1.toInt(), RECORD_NUMBER_4.toInt(), CONTRACT_RECORD_SIZE)

      // Read counters content
      cardTransaction.prepareReadCounter(SFI_Counter, COUNTER_RECORDS_NB)
      cardTransaction.processCommands()

      val efCounter = calypsoCard.getFileBySfi(SFI_Counter)

      val efContractParser = calypsoCard.getFileBySfi(SFI_Contracts)
      val contracts = mutableMapOf<Int, CardContract>()

      /*
       * Step 11 - For each contract:
       */
      efContractParser.data.allRecordsContent.forEach {
        /*
         * Step 12 - Unpack the contract
         */
        contracts[it.key] = CardContractParser().parse(it.value)
      }

      /*
       * Retrieve contract used for last event
       */
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
          /*
           * Step 13 - If the ContractVersionNumber == 0 then the contract is blank, move on to the next contract.
           */
        } else if (contract.contractVersionNumber != VersionNumber.CURRENT_VERSION) {
          /*
           * Step 14 - If ContractVersionNumber is not the expected one (==1 for the current version) reject the card.
           * <Abort Transaction if inTransactionFlag is true and exit process>
           */
        } else {
          /*
           * Step 15 - If SAM available and ContractAuthenticator is not 0 perform the verification of the value
           * by using the PSO Verify Signature command of the SAM.
           */
          @Suppress("ControlFlowWithEmptyBody")
          if (inTransactionFlag && contract.contractAuthenticator != 0) {
            /*
             * Step 15.1 - If the value is wrong reject the card.
             * <Abort Transaction if inTransactionFlag is true and exit process>
             */
            /*
             * Step 15.2 - If the value of ContractSaleSam is present in the SAM Black List reject the card.
             * <Abort Transaction if inTransactionFlag is true and exit process>
             */
            // TODO: steps 15.1 & 15.2
          }
          /*
           * Step 16 - If ContractValidityEndDate points to a date in the past mark contract as expired.
           */
          val contractValidityEndDate = DateTime(contract.getContractValidityEndDateAsDate())
          if (contractValidityEndDate.isBefore(now)) {
            contractExpired = true
          }

          /*
           * Step 17 - If EventContractUsed points to the current contract index
           * & not valid flag is false then mark it as Validated.
           */
          if (contractUsed == record && !contractEventNotValid) {
            contractValidated = true
          }

          var validationDate: DateTime? = null
          if (contractValidated && contractUsed == record) {
            validationDate = eventDateTime
          }

          /*
           * Step 18 -   If the ContractTariff value for the contract is 2 or 3, unpack the counter associated to the contract to extract the counter value.
           */
          val nbTicketsLeft =
              if (contract.contractTariff == ContractPriority.MULTI_TRIP) {
                efCounter.data.getContentAsCounterValue(record)
              } else {
                null
              }

          /*
           * Step 19 - Add contract data to the list of contracts read to return to the upper layer.
           */
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

      /*
       * Step 20 - If inTransactionFlag is true, Close the session
       */
      if (inTransactionFlag) {
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

      /*
       * Step 21 - Return the status of the operation to the upper layer. <Exit process>
       */
      return CardReaderResponse(
          status = status, lastValidationsList = validationList, titlesList = displayedContract)
    } catch (e: EnvironmentControlException) {
      Timber.e(e)
      errorMessage = e.message
    } catch (e: EventControlException) {
      Timber.e(e)
      errorMessage = e.message
      status =
          if (e.key == EventControlExceptionKey.CLEAN_CARD) {
            Status.EMPTY_CARD
          } else {
            Status.ERROR
          }
    } catch (e: ControlException) {
      Timber.e(e)
      errorTitle = e.title
      errorMessage = e.message
      status = Status.ERROR
    } catch (e: Exception) {
      Timber.e(e)
      errorMessage = e.message
      status = Status.ERROR
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
    const val CONTRACT_RECORD_SIZE = 0x1D
  }
}
