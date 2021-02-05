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
import org.eclipse.keyple.demo.control.exception.NoSamException
import org.eclipse.keyple.demo.control.models.Status
import org.eclipse.keyple.demo.control.ticketing.AbstractTicketingSession
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0A
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0B
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0C
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Counter_0D
import org.eclipse.keyple.demo.control.utils.CardletUtils
import org.eclipse.keyple.parser.keyple.CounterStructureParser
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import timber.log.Timber

/**
 *  @author youssefamrani
 */

class PersonalizeProcedure {

    fun launch(
        contractType: ContractPriorityEnum,
        calypsoPo: CalypsoPo,
        samReader: Reader?,
        ticketingSession: AbstractTicketingSession
    ): Status {

        val cardlet = when (contractType) {
            ContractPriorityEnum.SEASON_PASS -> CardletUtils.getSeasonPassCardlet()
            ContractPriorityEnum.MULTI_TRIP -> CardletUtils.getMultiTripCardlet()
            else -> throw IllegalArgumentException("You can only personalize SeasonPass and MultiTrip contracts")
        }

        val poReader = ticketingSession.poReader

        try {
            val poTransaction =
                if (samReader != null) {

                    PoTransaction(
                        CardResource(poReader, calypsoPo),
                        ticketingSession.getSecuritySettings(
                            ticketingSession.checkSamAndOpenChannel(
                                samReader
                            )
                        )
                    )
                } else {
                    throw NoSamException()
                }

            poTransaction.processOpening(PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_PERSO)

            /*
             * Environment
             */
            poTransaction.prepareUpdateRecord(
                CalypsoInfo.SFI_EnvironmentAndHolder, 1,
                cardlet.envData
            )

            /*
             * Event
             */
            poTransaction.prepareUpdateRecord(
                CalypsoInfo.SFI_EventLog, 1,
                cardlet.eventData[0]
            )

            /*
             * Contracts
             */
            poTransaction.prepareUpdateRecord(
                CalypsoInfo.SFI_Contracts,
                CalypsoInfo.RECORD_NUMBER_1.toInt(),
                cardlet.contractData[0]
            )
            poTransaction.prepareUpdateRecord(
                CalypsoInfo.SFI_Contracts,
                CalypsoInfo.RECORD_NUMBER_2.toInt(),
                cardlet.contractData[1]
            )
            poTransaction.prepareUpdateRecord(
                CalypsoInfo.SFI_Contracts,
                CalypsoInfo.RECORD_NUMBER_3.toInt(),
                cardlet.contractData[2]
            )

            /*
             * Counters
             */
            poTransaction.prepareUpdateRecord(
                SFI_Counter,
                CalypsoInfo.RECORD_NUMBER_1.toInt(),
                CardletUtils.getEmptyFile()
            )
            poTransaction.processPoCommands()

            if (contractType == ContractPriorityEnum.MULTI_TRIP) {
                poTransaction.prepareIncreaseCounter(
                    SFI_Counter,
                    CalypsoInfo.RECORD_NUMBER_1.toInt(),
                    CounterStructureParser().parse(cardlet.counterData).counterValue
                )
                poTransaction.processPoCommands()

                printCounterValues(poTransaction = poTransaction, calypsoPo = calypsoPo)

            }

            /*
             * Close Calypso session
             */
            poTransaction.processClosing()

            Timber.i("Personalization Successful - Calypso Session Closed.")
            return Status.SUCCESS
        } catch (e: CalypsoPoTransactionException) {
            Timber.e(e)
        } catch (e: CalypsoPoCommandException) {
            Timber.e(e)
        } catch (e: CalypsoSamCommandException) {
            Timber.e(e)
        }

        return Status.ERROR
    }

    private fun printCounterValues(poTransaction: PoTransaction, calypsoPo: CalypsoPo){

        poTransaction.prepareReadCounterFile(
            SFI_Counter_0A,
            CalypsoInfo.RECORD_NUMBER_1.toInt()
        )

        poTransaction.prepareReadCounterFile(
            SFI_Counter_0B,
            CalypsoInfo.RECORD_NUMBER_1.toInt()
        )

        poTransaction.prepareReadCounterFile(
            SFI_Counter_0C,
            CalypsoInfo.RECORD_NUMBER_1.toInt()
        )

        poTransaction.prepareReadCounterFile(
            SFI_Counter_0D,
            CalypsoInfo.RECORD_NUMBER_1.toInt()
        )
        poTransaction.processPoCommands()


        val counterContent0A = calypsoPo.getFileBySfi(SFI_Counter_0A)
            .data
            .allRecordsContent[CalypsoInfo.RECORD_NUMBER_1.toInt()]!!
        val counter0A = CounterStructureParser().parse(counterContent0A)
        println(">>> PersonalizeProcedure.launch - counter0A.counterValue : ${counter0A.counterValue}")
        println(">>> ")

        val counterContent0B = calypsoPo.getFileBySfi(SFI_Counter_0B)
            .data
            .allRecordsContent[CalypsoInfo.RECORD_NUMBER_1.toInt()]!!
        val counter0B = CounterStructureParser().parse(counterContent0B)
        println(">>> PersonalizeProcedure.launch - counter0B.counterValue : ${counter0B.counterValue}")
        println(">>> ")

        val counterContent0C = calypsoPo.getFileBySfi(SFI_Counter_0C)
            .data
            .allRecordsContent[CalypsoInfo.RECORD_NUMBER_1.toInt()]!!
        val counter0C = CounterStructureParser().parse(counterContent0C)
        println(">>> PersonalizeProcedure.launch - counter0C.counterValue : ${counter0C.counterValue}")
        println(">>> ")

        val counterContent0D = calypsoPo.getFileBySfi(SFI_Counter_0D)
            .data
            .allRecordsContent[CalypsoInfo.RECORD_NUMBER_1.toInt()]!!
        val counter0D = CounterStructureParser().parse(counterContent0D)
        println(">>> PersonalizeProcedure.launch - counter0D.counterValue : ${counter0D.counterValue}")
        println(">>> ")
    }
}
