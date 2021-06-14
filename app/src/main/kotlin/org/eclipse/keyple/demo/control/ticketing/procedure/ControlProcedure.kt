/*
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.keyple.demo.control.ticketing.procedure

import org.eclipse.keyple.calypso.command.po.exception.CalypsoPoCommandException
import org.eclipse.keyple.calypso.command.sam.exception.CalypsoSamCommandException
import org.eclipse.keyple.calypso.transaction.CalypsoPo
import org.eclipse.keyple.calypso.transaction.PoTransaction
import org.eclipse.keyple.calypso.transaction.exception.CalypsoPoTransactionException
import org.eclipse.keyple.core.card.selection.CardResource
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.demo.control.exception.ControlException
import org.eclipse.keyple.demo.control.exception.EnvironmentControlException
import org.eclipse.keyple.demo.control.exception.EnvironmentControlExceptionKey
import org.eclipse.keyple.demo.control.exception.EventControlException
import org.eclipse.keyple.demo.control.exception.EventControlExceptionKey
import org.eclipse.keyple.demo.control.exception.NoLocationDefinedException
import org.eclipse.keyple.demo.control.models.CardReaderResponse
import org.eclipse.keyple.demo.control.models.Contract
import org.eclipse.keyple.demo.control.models.KeypleSettings
import org.eclipse.keyple.demo.control.models.Location
import org.eclipse.keyple.demo.control.models.Status
import org.eclipse.keyple.demo.control.models.Validation
import org.eclipse.keyple.demo.control.models.mapper.ContractMapper
import org.eclipse.keyple.demo.control.models.mapper.ValidationMapper
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_1
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_2
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_3
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_4
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Contracts
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0A
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0B
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0C
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0D
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_EnvironmentAndHolder
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_EventLog
import org.eclipse.keyple.demo.control.ticketing.ITicketingSession
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
        calypsoPo: CalypsoPo,
        samReader: Reader?,
        ticketingSession: ITicketingSession,
        locations: List<Location>
    ): CardReaderResponse {
        val now = DateTime.now()
//        val now = DateTime()
//            .withTimeAtStartOfDay()
//            .withYear(2021)
//            .withMonthOfYear(1)
//            .withDayOfMonth(14)
//            .withHourOfDay(15)
//            .withMinuteOfHour(30)

        val poReader = ticketingSession.poReader
        val poTypeName = ticketingSession.poTypeName

        val errorMessage: String?
        var errorTitle: String? = null
        var validation: Validation? = null
        var status: Status = Status.ERROR
        try {
            var inTransactionFlag: Boolean //true if a SAM is available and a secure session have been opened
            val poTransaction =
                try {
                    if (samReader != null) {
                        /*
                         * Step 1.1 - If SAM available, Open a Validation session reading the environment record, set inTransactionFlag to true and go to point 2.
                         */
                        inTransactionFlag = true
                        val cardResource = ticketingSession.checkSamAndOpenChannel(samReader)
                        PoTransaction(
                            CardResource(ticketingSession.poReader, calypsoPo),
                            ticketingSession.getSecuritySettings(cardResource)
                        )
                    } else {
                        /*
                         * Step 1.2 - Else, read the environment record.
                         */
                        inTransactionFlag = false
                        PoTransaction(CardResource(poReader, calypsoPo))
                    }
                } catch (e: IllegalStateException) {
                    Timber.w(e)
                    inTransactionFlag = false
                    PoTransaction(CardResource(poReader, calypsoPo))
                }

            /*
             * Step 2 - Unpack environment structure from the binary present in the environment record.
             */
            poTransaction.prepareReadRecordFile(
                SFI_EnvironmentAndHolder,
                RECORD_NUMBER_1.toInt()
            )

            if (inTransactionFlag) {
                /*
                 * Open a transaction to read/write the Calypso PO and read the Environment file
                 */
                poTransaction.processOpening(PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_DEBIT)
            }
            else{
                /*
                 * Read the Environment file
                 */
                poTransaction.processPoCommands()
            }

            val efEnvironmentHolder =
                calypsoPo.getFileBySfi(SFI_EnvironmentAndHolder)
            val env = EnvironmentHolderStructureParser().parse(efEnvironmentHolder.data.content)

            /*
             * Step 3 - If EnvVersionNumber of the Environment structure is not the expected one (==1 for the current version) reject the card.
             * <Abort Transaction if inTransactionFlag is true and exit process>
             */
            if (env.envVersionNumber != VersionNumberEnum.CURRENT_VERSION.key) {
                if (inTransactionFlag) {
                    poTransaction.processClosing()
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
                    poTransaction.processClosing()
                }
                throw EnvironmentControlException(EnvironmentControlExceptionKey.EXPIRED)
            }

            /*
             * Step 5 - Read and unpack the last event record.
             */
            poTransaction.prepareReadRecordFile(
                SFI_EventLog,
                RECORD_NUMBER_1.toInt()
            )
            poTransaction.processPoCommands()

            val efEventLog = calypsoPo.getFileBySfi(SFI_EventLog)
            val event = EventStructureParser().parse(efEventLog.data.content)

            /*
             * Step 6 - If EventVersionNumber is not the expected one (==1 for the current version) reject the card
             * (if ==0 return error status indicating clean card).
             * <Abort Transaction if inTransactionFlag is true and exit process>
             */
            val eventVersionNumber = event.eventVersionNumber
            if (eventVersionNumber != VersionNumberEnum.CURRENT_VERSION.key) {
                if (inTransactionFlag) {
                    poTransaction.processClosing()
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
            poTransaction.prepareReadRecordFile(
                SFI_Contracts,
                RECORD_NUMBER_1.toInt()
            )
            poTransaction.prepareReadRecordFile(
                SFI_Contracts,
                RECORD_NUMBER_2.toInt()
            )
            poTransaction.prepareReadRecordFile(
                SFI_Contracts,
                RECORD_NUMBER_3.toInt()
            )
            poTransaction.processPoCommands()

            val efContractParser = calypsoPo.getFileBySfi(SFI_Contracts)
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
                        //TODO: steps 14.1 & 14.2
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

                            poTransaction.prepareReadRecordFile(
                                counterSfi,
                                RECORD_NUMBER_1.toInt()
                            )
                            poTransaction.processPoCommands()

                            val efCounter = calypsoPo.getFileBySfi(counterSfi)
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
                poTransaction.processClosing()
            }

            var validationList: ArrayList<Validation>? = null
            if (validation != null) {
                validationList = arrayListOf(validation)
            }

            Timber.i("Control procedure result : STATUS_OK")
            status = Status.TICKETS_FOUND
            return CardReaderResponse(
                status = status,
                cardType = poTypeName ?: "",
                lastValidationsList = validationList,
                titlesList = displayedContract
            )
        } catch (e: CalypsoSamCommandException) {
            Timber.e(e)
            errorMessage = e.message
        } catch (e: CalypsoPoCommandException) {
            Timber.e(e)
            errorMessage = e.message
        } catch (e: CalypsoPoTransactionException) {
            Timber.e(e)
            errorMessage = e.message
            status = Status.ERROR
        } catch (e: EventControlException) {
            Timber.e(e)
            errorMessage = e.message
            status = if(e.key == EventControlExceptionKey.CLEAN_CARD){
                Status.EMPTY_CARD
            }
            else{
                Status.ERROR
            }
        } catch (e: ControlException) {
            Timber.e(e)
            errorTitle = e.title
            errorMessage = e.message
        } catch (e: Exception) {
            Timber.e(e)
            errorMessage = e.message
            status = Status.ERROR
        }

        return CardReaderResponse(
            status = status,
            titlesList = arrayListOf(),
            errorTitle = errorTitle,
            errorMessage = errorMessage,
            cardType = ticketingSession.poTypeName
        )
    }

    /**
     * An event is considered valid for display if an eventTimeStamp or an eventDateStamp has been set during a provious validation
     */
    private fun isValidEvent(event: EventStructureDto): Boolean {
        return event.eventTimeStamp != 0 || event.eventDateStamp != 0
    }
}
