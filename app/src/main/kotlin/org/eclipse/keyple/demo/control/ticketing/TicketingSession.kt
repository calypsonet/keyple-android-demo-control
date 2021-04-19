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

import org.eclipse.keyple.card.calypso.CalypsoCardExtensionProvider
import org.eclipse.keyple.card.calypso.po.PoSmartCard
import org.eclipse.keyple.card.calypso.sam.SamRevision
import org.eclipse.keyple.card.calypso.transaction.CalypsoPoTransactionException
import org.eclipse.keyple.card.calypso.transaction.PoTransactionService
import org.eclipse.keyple.core.card.ProxyReader
import org.eclipse.keyple.core.common.KeypleCardSelectionResponse
import org.eclipse.keyple.core.service.CardSelectionServiceFactory
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.selection.CardSelectionResult
import org.eclipse.keyple.core.service.selection.CardSelector
import org.eclipse.keyple.core.service.selection.MultiSelectionProcessing
import org.eclipse.keyple.demo.control.di.scopes.AppScoped
import org.eclipse.keyple.demo.control.exception.ControlException
import org.eclipse.keyple.demo.control.models.CardReaderResponse
import org.eclipse.keyple.demo.control.models.Location
import org.eclipse.keyple.demo.control.models.Status
import org.eclipse.keyple.demo.control.reader.IReaderRepository
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.AID_HIS_STRUCTURE_5H
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.AID_NORMALIZED_IDF
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_BANKING
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_CALYPSO_05H
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_NAVIGO
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_OTHER
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.RECORD_NUMBER_1
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_Contracts
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SFI_EventLog
import org.eclipse.keyple.demo.control.ticketing.procedure.ControlProcedure
import org.eclipse.keyple.demo.control.ticketing.procedure.PersonalizeProcedure
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import javax.inject.Inject

@AppScoped
class TicketingSession @Inject constructor(readerRepository: IReaderRepository) :
    AbstractTicketingSession(readerRepository), ITicketingSession {

    private var samReader: Reader? = null

    init {
        samReader = readerRepository.getSamReader()
    }

    private var navigoCardIndex = 0
    private var bankingCardIndex = 0

    /*
     * Should be instanciated through the ticketing session mananger
    */
    init {
        prepareAndSetPoDefaultSelection()
    }

    /**
     * prepare the default selection
     */
    fun prepareAndSetPoDefaultSelection() {
        /*
         * Prepare a PO selection
         */
        cardSelection = CardSelectionServiceFactory.getService(MultiSelectionProcessing.FIRST_MATCH)
        calypsoCardExtensionProvider = CalypsoCardExtensionProvider.getService()

        /* Select Calypso */
        val poSelectionRequest =
            calypsoCardExtensionProvider.createPoCardSelection(
                CardSelector.builder()
                    .filterByDfName(AID_HIS_STRUCTURE_5H)
                    .filterByCardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)
                    .build(),
                false
            )

        /*
         * Add the selection case to the current selection
         */
        calypsoPoIndex = cardSelection.prepareSelection(poSelectionRequest)

        /*
         * NAVIGO
         */
        val navigoCardSelectionRequest =
            calypsoCardExtensionProvider.createPoCardSelection(
                CardSelector.builder()
                    .filterByDfName(AID_NORMALIZED_IDF)
                    .filterByCardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)
                    .build(),
                false
            )
        navigoCardIndex = cardSelection.prepareSelection(navigoCardSelectionRequest)

        /*
         * Banking
         */
//        val bankingCardSelectionRequest = GenericSeSelectionRequest(
//            PoSelector.builder()
//                .cardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)
//                .aidSelector(
//                    CardSelector.AidSelector.builder().aidToSelect(AID_BANKING)
//                        .build()
//                )
//                .invalidatedPo(PoSelector.InvalidatedPo.REJECT).build()
//        )
//        bankingCardIndex = cardSelection.prepareSelection(bankingCardSelectionRequest)

        /*
         * Provide the Reader with the selection operation to be processed when a PO is inserted.
         */

        cardSelection.scheduleCardSelectionScenario(
            poReader as ObservableReader,
            ObservableReader.NotificationMode.ALWAYS
        )
    }

    fun processDefaultSelection(selectionResponse: List<KeypleCardSelectionResponse>): CardSelectionResult {
        Timber.i("selectionResponse = $selectionResponse")
        val selectionsResult =
            cardSelection.processCardSelectionResponses(selectionResponse)

        if (selectionsResult.hasActiveSelection()) {
            when (selectionsResult.smartCards.keys.first()) {
                calypsoPoIndex -> {
                    calypsoPo = selectionsResult.activeSmartCard as PoSmartCard
                    poTypeName = PO_TYPE_NAME_CALYPSO_05H
                }
                navigoCardIndex -> {
                    calypsoPo = selectionsResult.activeSmartCard as PoSmartCard
                    poTypeName = PO_TYPE_NAME_NAVIGO
                }
                bankingCardIndex -> poTypeName = PO_TYPE_NAME_BANKING
                else -> poTypeName = PO_TYPE_NAME_OTHER
            }
        }

        Timber.i("PO type = $poTypeName")
        return selectionsResult
    }

    /*
     * public void forceCloseChannel() throws KeypleReaderException {
     * logger.debug("Force close logical channel (hack for nfc reader)"); List<ApduRequest>
     * requestList = new ArrayList<>(); ((ProxyReader)poReader).transmit(new
     * SeRequest(requestList)); }
     */
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
            val poTransaction = if (samReader != null) {

                val samCardResourceProfileExtension =
                    calypsoCardExtensionProvider.createSamCardResourceProfileExtension()
                samCardResourceProfileExtension.setSamRevision(SamRevision.C1)

                calypsoCardExtensionProvider.createPoSecuredTransaction(
                    poReader,
                    calypsoPo,
                    getSecuritySettings(),
                    samCardResourceProfileExtension,
                    samReader as ProxyReader
                )
            } else {
                calypsoCardExtensionProvider.createPoUnsecuredTransaction(
                    poReader,
                    calypsoPo
                )
            }
            if (!Arrays.equals(currentPoSN, calypsoPo.applicationSerialNumberBytes)) {
                Timber.i("Load ticket status  : STATUS_CARD_SWITCHED")
                return ITicketingSession.STATUS_CARD_SWITCHED
            }
            /*
             * Open a transaction to read/write the Calypso PO
             */
            poTransaction.processOpening(PoTransactionService.SessionAccessLevel.SESSION_LVL_LOAD)

            /*
             * Read actual ticket number
             */
            poTransaction.prepareReadRecordFile(
                CalypsoInfo.SFI_Counter,
                RECORD_NUMBER_1.toInt()
            )
            poTransaction.processPoCommands()
            poTransaction.prepareIncreaseCounter(
                CalypsoInfo.SFI_Counter,
                RECORD_NUMBER_1.toInt(),
                ticketNumber
            )

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
            poTransaction.prepareAppendRecord(SFI_EventLog, event.toByteArray())

            /*
             * Process transaction
             */
            cardSelection.prepareReleaseChannel()
            Timber.i("Load ticket status  : STATUS_OK")
            ITicketingSession.STATUS_OK
        } catch (e: CalypsoPoTransactionException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        } catch (e: Exception) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        }
    }


    /**
     * Launch the control procedure of the current PO
     *
     * @return
     */
    @Throws(
        CalypsoPoTransactionException::class,
        Exception::class,
        ControlException::class
    )
    fun launchControlProcedure(locations: List<Location>): CardReaderResponse? {
        return ControlProcedure().launch(
            calypsoPo = calypsoPo,
            samReader = samReader,
            ticketingSession = this@TicketingSession,
            locations = locations
        )
    }

    /**
     * Launch the personalization of the current PO
     *
     * @return
     */
    @Throws(
        CalypsoPoTransactionException::class,
        Exception::class,
        ControlException::class
    )
    fun launchPersonalizeProcedure(contractType: ContractPriorityEnum): Status {
        return PersonalizeProcedure().launch(
            contractType = contractType,
            calypsoPo = calypsoPo,
            samReader = samReader,
            ticketingSession = this
        )
    }

    /**
     * Load a season ticket contract
     *
     * @return
     * @throws KeypleReaderException
     */
    @Throws(Exception::class)
    fun loadContract(): Int {
        return try {
            val poTransaction = if (samReader != null) {

                val samCardResourceProfileExtension =
                    calypsoCardExtensionProvider.createSamCardResourceProfileExtension()
                samCardResourceProfileExtension.setSamRevision(SamRevision.C1)

                calypsoCardExtensionProvider.createPoSecuredTransaction(
                    poReader,
                    calypsoPo,
                    getSecuritySettings(),
                    samCardResourceProfileExtension,
                    samReader as ProxyReader
                )
            } else {
                calypsoCardExtensionProvider.createPoUnsecuredTransaction(
                    poReader,
                    calypsoPo
                )
            }

            if (!Arrays.equals(currentPoSN, calypsoPo.applicationSerialNumberBytes)) {
                return ITicketingSession.STATUS_CARD_SWITCHED
            }

            poTransaction.processOpening(PoTransactionService.SessionAccessLevel.SESSION_LVL_LOAD)

            /* allow to determine the anticipated response */
            poTransaction.prepareReadRecordFile(
                CalypsoInfo.SFI_Counter,
                RECORD_NUMBER_1.toInt()
            )
            poTransaction.processPoCommands()
            poTransaction.prepareUpdateRecord(
                SFI_Contracts,
                RECORD_NUMBER_1.toInt(),
                pad("1 MONTH SEASON TICKET", ' ', 29).toByteArray()
            )

            // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("")
            // String dateTime = LocalDateTime.now().format(formatter)
            val dateFormat: DateFormat = SimpleDateFormat("yyMMdd HH:mm:ss")
            val event =
                pad(dateFormat.format(Date()) + " OP = +ST", ' ', 29)
            poTransaction.prepareAppendRecord(SFI_EventLog, event.toByteArray())
            poTransaction.processClosing()
            ITicketingSession.STATUS_OK
        } catch (e: CalypsoPoTransactionException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        } catch (e: Exception) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        }
    }

//    /**
//     * Create a new class extending AbstractSeSelectionRequest
//     */
//    inner class GenericSeSelectionRequest(seSelector: CardSelector) :
//        AbstractCardSelection<AbstractApduCommandBuilder>(seSelector) {
//        override fun parse(seResponse: CardSelectionResponse): PoSmartCard {
//            class GenericMatchingSe(
//                selectionResponse: CardSelectionResponse?
//            ) : AbstractSmartCard(selectionResponse)
//            return GenericMatchingSe(seResponse)
//        }
//    }
}
