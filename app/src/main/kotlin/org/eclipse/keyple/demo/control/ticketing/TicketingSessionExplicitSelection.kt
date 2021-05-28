/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.demo.control.ticketing

import org.eclipse.keyple.card.calypso.transaction.CardTransactionService
import org.eclipse.keyple.core.service.CardSelectionServiceFactory
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.selection.CardSelectionResult
import org.eclipse.keyple.core.service.selection.CardSelector
import org.eclipse.keyple.core.service.selection.MultiSelectionProcessing
import org.eclipse.keyple.demo.control.reader.IReaderRepository
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date

class TicketingSessionExplicitSelection(readerRepository: IReaderRepository) :
    AbstractTicketingSession(readerRepository), ITicketingSession {

    private var samReader: Reader? = null

    init {
        samReader = readerRepository.getSamReader()
    }

    /**
     * prepare the default selection
     */
    @Throws(Exception::class)
    fun processExplicitSelection(): CardSelectionResult {
        /*
         * Prepare a PO selection
         */
        cardSelection = CardSelectionServiceFactory.getService(MultiSelectionProcessing.FIRST_MATCH)

        /* Select Calypso */
        val poSelectionRequest =
            calypsoCardExtensionProvider.createCardSelection(
                CardSelector.builder()
                    .filterByDfName(CalypsoInfo.AID_HISTORIC)
                    .filterByCardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)
                    .build(),
                false
            )

        // Prepare the reading of the Environment and Holder file.
        poSelectionRequest.prepareReadRecordFile(CalypsoInfo.SFI_EnvironmentAndHolder, CalypsoInfo.RECORD_NUMBER_1.toInt())
        poSelectionRequest.prepareReadRecordFile(CalypsoInfo.SFI_Contracts, CalypsoInfo.RECORD_NUMBER_1.toInt())
        poSelectionRequest.prepareReadRecordFile(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1.toInt())
        poSelectionRequest.prepareReadRecordFile(CalypsoInfo.SFI_EventLog, CalypsoInfo.RECORD_NUMBER_1.toInt())

        /*
         * Add the selection case to the current selection (we could have added other cases here)
         */
        calypsoPoIndex = cardSelection.prepareSelection(poSelectionRequest)

        cardSelection.scheduleCardSelectionScenario(
            poReader as ObservableReader,
            ObservableReader.NotificationMode.ALWAYS
        )

        return cardSelection.processCardSelectionScenario(poReader)
//        return cardSelection.processExplicitSelections(poReader)
    }

    /**
     * load the PO according to the choice provided as an argument
     *
     * @param ticketNumber
     * @return
     * @throws KeypleReaderException
     */
    @Throws(Exception::class)
    override fun loadTickets(ticketNumber: Int): Int {
        return try {
            /**
             * Open channel (again?)
             */
            val selectionsResult = processExplicitSelection()

            /* No sucessful selection */
            if (!selectionsResult.hasActiveSelection()) {
                Timber.e("PO Not selected")
                return ITicketingSession.STATUS_SESSION_ERROR
            }

            val poTransaction = if (samReader != null) {
                //TODO: remove useless code
                calypsoCardExtensionProvider.createCardTransaction(
                    poReader,
                    calypsoCard,
                    getSecuritySettings()
                )
            } else {
                calypsoCardExtensionProvider.createCardTransactionWithoutSecurity(
                    poReader,
                    calypsoCard
                )
            }

            if (!Arrays.equals(currentPoSN, calypsoCard.applicationSerialNumberBytes)) {
                Timber.i("Load ticket status  : STATUS_CARD_SWITCHED")
                return ITicketingSession.STATUS_CARD_SWITCHED
            }

            /*
             * Open a transaction to read/write the Calypso PO
             */
            poTransaction.processOpening(CardTransactionService.SessionAccessLevel.SESSION_LVL_LOAD)

            /*
             * Read actual ticket number
             */
            poTransaction.prepareReadRecordFile(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1.toInt())
            poTransaction.processCardCommands()

            poTransaction.prepareIncreaseCounter(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1.toInt(), ticketNumber)

            /*
             * Prepare record to be sent to Calypso PO log journal
             */
            val dateFormat: DateFormat = SimpleDateFormat("yyMMdd HH:mm:ss")
            val dateTime = dateFormat.format(Date())
            var event = ""
            event = if (ticketNumber > 0) {
                pad("$dateTime OP = +$ticketNumber", ' ', 29)
            } else {
                pad("$dateTime T1", ' ', 29)
            }

            poTransaction.prepareAppendRecord(CalypsoInfo.SFI_EventLog, event.toByteArray())

            /*
             * Process transaction
             */
            poTransaction.processClosing()

            Timber.i("Load ticket status  : STATUS_OK")
            ITicketingSession.STATUS_OK
        } catch (e: Exception) {
            e.printStackTrace()
            ITicketingSession.STATUS_UNKNOWN_ERROR
        }
    }
}
