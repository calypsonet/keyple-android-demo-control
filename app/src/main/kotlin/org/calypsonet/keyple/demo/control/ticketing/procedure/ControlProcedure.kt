/********************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.calypsonet.keyple.demo.control.ticketing.procedure

import org.calypsonet.keyple.demo.control.exception.ControlException
import org.calypsonet.keyple.demo.control.exception.EnvironmentControlException
import org.calypsonet.keyple.demo.control.exception.EnvironmentControlExceptionKey
import org.calypsonet.keyple.demo.control.exception.EventControlException
import org.calypsonet.keyple.demo.control.exception.EventControlExceptionKey
import org.calypsonet.keyple.demo.control.exception.NoLocationDefinedException
import org.calypsonet.keyple.demo.control.models.CardReaderResponse
import org.calypsonet.keyple.demo.control.models.Contract
import org.calypsonet.keyple.demo.control.models.KeypleSettings
import org.calypsonet.keyple.demo.control.models.Location
import org.calypsonet.keyple.demo.control.models.Status
import org.calypsonet.keyple.demo.control.models.Validation
import org.calypsonet.keyple.demo.control.models.mapper.ContractMapper
import org.calypsonet.keyple.demo.control.models.mapper.ValidationMapper
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_1
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_2
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_3
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_4
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SAM_PROFILE_NAME
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_Contracts
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0A
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0B
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0C
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0D
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_EnvironmentAndHolder
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SFI_EventLog
import org.calypsonet.keyple.demo.control.ticketing.ITicketingSession
import org.calypsonet.terminal.calypso.WriteAccessLevel
import org.calypsonet.terminal.calypso.card.CalypsoCard
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.parser.keyple.ContractStructureParser
import org.eclipse.keyple.parser.keyple.CounterStructureParser
import org.eclipse.keyple.parser.keyple.EnvironmentHolderStructureParser
import org.eclipse.keyple.parser.keyple.EventStructureParser
import org.eclipse.keyple.parser.model.ContractStructureDto
import org.eclipse.keyple.parser.model.EventStructureDto
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import org.eclipse.keyple.parser.model.type.VersionNumberEnum
import org.joda.time.DateTime
import timber.log.Timber

/**
 *  @author youssefamrani
 */

class ControlProcedure {

    fun launch(
        now: DateTime,
        calypsoCard: CalypsoCard,
        samReader: Reader?,
        ticketingSession: ITicketingSession,
        locations: List<Location>
    ): CardReaderResponse {

        val cardReader = ticketingSession.cardReader

        val errorMessage: String?
        var errorTitle: String? = null
        var validation: Validation? = null
        var status: Status = Status.ERROR

        val calypsoCardExtensionProvider = CalypsoExtensionService.getInstance()

        try {
            var inTransactionFlag: Boolean // true if a SAM is available and a secure session have been opened
            val cardTransaction =
                try {
                    if (samReader != null) {
                        /*
                         * Step 1.1 - If SAM available, Open a Validation session reading the environment record, set inTransactionFlag to true and go to point 2.
                         */
                        inTransactionFlag = true

                        ticketingSession.setupCardResourceService(SAM_PROFILE_NAME)

                        calypsoCardExtensionProvider.createCardTransaction(
                            cardReader,
                            calypsoCard,
                            ticketingSession.getSecuritySettings()
                        )
                    } else {
                        /*
                         * Step 1.2 - Else, read the environment record.
                         */
                        inTransactionFlag = false
                        calypsoCardExtensionProvider.createCardTransactionWithoutSecurity(
                            cardReader,
                            calypsoCard
                        )
                    }
                } catch (e: IllegalStateException) {
                    Timber.w(e)
                    inTransactionFlag = false
                    calypsoCardExtensionProvider.createCardTransactionWithoutSecurity(
                        cardReader,
                        calypsoCard
                    )
                } catch (e: Exception) {
                    Timber.w(e)
                    inTransactionFlag = false
                    calypsoCardExtensionProvider.createCardTransactionWithoutSecurity(
                        cardReader,
                        calypsoCard
                    )
                }

            /*
             * Step 2 - Unpack environment structure from the binary present in the environment record.
             */
            cardTransaction.prepareReadRecordFile(
                SFI_EnvironmentAndHolder,
                RECORD_NUMBER_1.toInt()
            )

            if (inTransactionFlag) {
                /*
                 * Open a transaction to read/write the Calypso Card and read the Environment file
                 */
                cardTransaction.processOpening(WriteAccessLevel.DEBIT)
            } else {
                /*
                 * Read the Environment file
                 */
                cardTransaction.processCardCommands()
            }

            val efEnvironmentHolder =
                calypsoCard.getFileBySfi(SFI_EnvironmentAndHolder)
            val env = EnvironmentHolderStructureParser().parse(efEnvironmentHolder.data.content)

            /*
             * Step 3 - If EnvVersionNumber of the Environment structure is not the expected one (==1 for the current version) reject the card.
             * <Abort Transaction if inTransactionFlag is true and exit process>
             */
            if (env.envVersionNumber != VersionNumberEnum.CURRENT_VERSION.key) {
                if (inTransactionFlag) {
                    cardTransaction.processClosing()
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
                    cardTransaction.processClosing()
                }
                throw EnvironmentControlException(EnvironmentControlExceptionKey.EXPIRED)
            }

            /*
             * Step 5 - Read and unpack the last event record.
             */
            cardTransaction.prepareReadRecordFile(
                SFI_EventLog,
                RECORD_NUMBER_1.toInt()
            )
            cardTransaction.processCardCommands()

            val efEventLog = calypsoCard.getFileBySfi(SFI_EventLog)
            val event = EventStructureParser().parse(efEventLog.data.content)

            /*
             * Step 6 - If EventVersionNumber is not the expected one (==1 for the current version) reject the card
             * (if ==0 return error status indicating clean card).
             * <Abort Transaction if inTransactionFlag is true and exit process>
             */
            val eventVersionNumber = event.eventVersionNumber
            if (eventVersionNumber != VersionNumberEnum.CURRENT_VERSION.key) {
                if (inTransactionFlag) {
                    cardTransaction.processClosing()
                }
                if (eventVersionNumber == VersionNumberEnum.UNDEFINED.key) {
                    throw EventControlException(EventControlExceptionKey.CLEAN_CARD)
                } else {
                    throw EventControlException(EventControlExceptionKey.WRONG_VERSION_NUMBER)
                }
            }

            var contratEventNotValid = false
            val contracUsed = event.eventContractUsed

            val eventDateTime = DateTime(event.getEventDate())
            val eventValidityEndDate =
                eventDateTime.plusMinutes(KeypleSettings.validationPeriod ?: 0)

            /*
             * Step 7 - If EventLocation != value configured in the control terminal set the validated contract not valid flag as true and go to point CNT_READ.
             */
            if (KeypleSettings.location == null) {
                throw NoLocationDefinedException()
            } else if (KeypleSettings.location!!.id != event.eventLocation) {
                contratEventNotValid = true
            }
            /*
             * Step 8 - Else If EventDateStamp points to a date in the past
             * -> set the validated contract not valid flag as true and go to point CNT_READ.
             */
            else if (eventDateTime.withTimeAtStartOfDay()
                    .isBefore(now.withTimeAtStartOfDay())
            ) {
                contratEventNotValid = true
            }

            /*
             * Step 9 - Else If (EventTimeStamp + Validation period configure in the control terminal) < current time of the control terminal
             *  -> set the validated contract not valid flag as true.
             */
            else if (eventValidityEndDate.isBefore(now)) {
                contratEventNotValid = true
            }

            /*
             * Step 10 - CNT_READ: For each contract:
             */
            cardTransaction.prepareReadRecordFile(
                SFI_Contracts,
                RECORD_NUMBER_1.toInt()
            )
            cardTransaction.prepareReadRecordFile(
                SFI_Contracts,
                RECORD_NUMBER_2.toInt()
            )
            cardTransaction.prepareReadRecordFile(
                SFI_Contracts,
                RECORD_NUMBER_3.toInt()
            )
            cardTransaction.processCardCommands()

            val efContractParser = calypsoCard.getFileBySfi(SFI_Contracts)
            val contracts = mutableMapOf<Int, ContractStructureDto>()
            efContractParser.data.allRecordsContent.forEach {
                /*
                 * Step 11 - Read and unpack the contract
                 */
                contracts[it.key] = ContractStructureParser().parse(it.value)
            }

            /*
             * Retrieve contract used for last event
             */
            val eventContract = contracts.toList().filter {
                it.first == event.eventContractUsed
            }.map {
                it.second
            }

            if (isValidEvent(event))
                if (eventContract.isNotEmpty()) {
                    validation = ValidationMapper.map(
                        event = event,
                        contract = eventContract[0],
                        locations = locations
                    )
                } else {
                    validation = ValidationMapper.map(
                        event = event,
                        contract = null,
                        locations = locations
                    )
                }

            val displayedContract = arrayListOf<Contract>()
            contracts.forEach {
                val record = it.key
                val contract = it.value
                var contractExpired = false
                var contractValidated = false

                if (contract.contractVersionNumber == VersionNumberEnum.UNDEFINED) {
                    /*
                     * Step 12 - If the ContractVersionNumber == 0 then the contract is blank, move on to the next contract.
                     */
                } else if (contract.contractVersionNumber != VersionNumberEnum.CURRENT_VERSION) {
                    /*
                     * Step 13 - If ContractVersionNumber is not the expected one (==1 for the current version) reject the card.
                     * <Abort Transaction if inTransactionFlag is true and exit process>
                     */
                } else {
                    /*
                     * Step 14 - If SAM available and ContractAuthenticator is not 0 perform the verification of the value
                     * by using the PSO Verify Signature command of the SAM.
                     */
                    @Suppress("ControlFlowWithEmptyBody")
                    if (inTransactionFlag && contract.contractAuthenticator != 0) {
                        /*
                         * Step 14.1 - If the value is wrong reject the card.
                         * <Abort Transaction if inTransactionFlag is true and exit process>
                         */
                        /*
                         * Step 14.2 - If the value of ContractSaleSam is present in the SAM Black List reject the card.
                         * <Abort Transaction if inTransactionFlag is true and exit process>
                         */
                        // TODO: steps 14.1 & 14.2
                    }
                    /*
                     * Step 15 - If ContractValidityEndDate points to a date in the past mark contract as expired.
                     */
                    val contractValidityEndDate =
                        DateTime(contract.getContractValidityEndDateAsDate())
                    if (contractValidityEndDate.isBefore(now)) {
                        contractExpired = true
                    }

                    /*
                     * Step 16 - If EventContractUsed points to the current contract index
                     * & not valid flag is false then mark it as Validated.
                     */
                    if (contracUsed == record && !contratEventNotValid) {
                        contractValidated = true
                    }

                    var validationDate: DateTime? = null
                    if (contractValidated && contracUsed == record) {
                        validationDate = eventDateTime
                    }

                    /*
                     * Step 16.1 - If EventContractUsed points to the current contract index
                     * & not valid flag is false then mark it as Validated.
                     * //TODO: Add this step to control procedure in order to determine the amount of trips left
                     */
                    val nbTicketsLeft =
                        if (contract.contractTariff == ContractPriorityEnum.MULTI_TRIP) {
                            val counterSfi = when (record) {
                                RECORD_NUMBER_1.toInt() -> SFI_Counter_0A
                                RECORD_NUMBER_2.toInt() -> SFI_Counter_0B
                                RECORD_NUMBER_3.toInt() -> SFI_Counter_0C
                                RECORD_NUMBER_4.toInt() -> SFI_Counter_0D
                                else -> throw IllegalStateException("Unhandled counter record number : $record")
                            }

                            cardTransaction.prepareReadRecordFile(
                                counterSfi,
                                RECORD_NUMBER_1.toInt()
                            )
                            cardTransaction.processCardCommands()

                            val efCounter = calypsoCard.getFileBySfi(counterSfi)
                            val counterContent = efCounter.data.allRecordsContent[1]!!

                            CounterStructureParser().parse(counterContent).counterValue
                        } else {
                            null
                        }

                    /*
                     * Step 17 - Add contract data to the list of contracts read to return to the upper layer.
                     */
                    displayedContract.add(
                        ContractMapper.map(
                            contract = contract,
                            record = record,
                            contractExpired = contractExpired,
                            contractValidated = contractValidated,
                            validationDate = validationDate,
                            nbTicketsLeft = nbTicketsLeft
                        )
                    )
                }
            }

            /*
             * Step 18 - If inTransactionFlag is true, Close the session
             */
            if (inTransactionFlag) {
                if(status == Status.SUCCESS){
                    cardTransaction.processClosing()
                }
                else{
                    cardTransaction.processCancel()
                }
            }

            var validationList: ArrayList<Validation>? = null
            if (validation != null) {
                validationList = arrayListOf(validation)
            }

            Timber.i("Control procedure result : STATUS_OK")
            status = Status.TICKETS_FOUND
            return CardReaderResponse(
                status = status,
                lastValidationsList = validationList,
                titlesList = displayedContract
            )
        } catch (e: EnvironmentControlException) {
            Timber.e(e)
            errorMessage = e.message
        } catch (e: EventControlException) {
            Timber.e(e)
            errorMessage = e.message
            status = if (e.key == EventControlExceptionKey.CLEAN_CARD) {
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
            errorMessage = errorMessage
        )
    }

    /**
     * An event is considered valid for display if an eventTimeStamp or an eventDateStamp has been set during a provious validation
     */
    private fun isValidEvent(event: EventStructureDto): Boolean {
        return event.eventTimeStamp != 0 || event.eventDateStamp != 0
    }
}
