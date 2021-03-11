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
package org.calypsonet.keyple.demo.control.ticketing

import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.calypso.command.po.exception.CalypsoPoCommandException
import org.eclipse.keyple.calypso.command.sam.SamRevision
import org.eclipse.keyple.calypso.command.sam.exception.CalypsoSamCommandException
import org.eclipse.keyple.calypso.transaction.CalypsoPo
import org.eclipse.keyple.calypso.transaction.CalypsoSam
import org.eclipse.keyple.calypso.transaction.PoSecuritySettings
import org.eclipse.keyple.calypso.transaction.PoSelection
import org.eclipse.keyple.calypso.transaction.PoSelector
import org.eclipse.keyple.calypso.transaction.PoTransaction
import org.eclipse.keyple.calypso.transaction.SamSelection
import org.eclipse.keyple.calypso.transaction.SamSelector
import org.eclipse.keyple.calypso.transaction.exception.CalypsoPoTransactionException
import org.eclipse.keyple.core.card.selection.CardResource
import org.eclipse.keyple.core.card.selection.CardSelectionsResult
import org.eclipse.keyple.core.card.selection.CardSelectionsService
import org.eclipse.keyple.core.card.selection.CardSelector
import org.eclipse.keyple.core.card.selection.MultiSelectionProcessing
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.event.AbstractDefaultSelectionsResponse
import org.eclipse.keyple.core.service.event.ObservableReader
import org.eclipse.keyple.core.service.exception.KeypleReaderException
import org.calypsonet.keyple.demo.control.di.scopes.AppScoped
import org.calypsonet.keyple.demo.control.exception.ControlException
import org.calypsonet.keyple.demo.control.models.CardReaderResponse
import org.calypsonet.keyple.demo.control.models.Location
import org.calypsonet.keyple.demo.control.models.StructureEnum
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_HIS_STRUCTURE_32H
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_HIS_STRUCTURE_5H
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_NORMALIZED_IDF_05H
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_CALYPSO_05h
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_CALYPSO_32h
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_NAVIGO_05h
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_OTHER
import org.calypsonet.keyple.demo.control.ticketing.procedure.ControlProcedure
import org.joda.time.DateTime
import timber.log.Timber
import java.util.EnumMap
import javax.inject.Inject

@AppScoped
class TicketingSession @Inject constructor(private val readerRepository: IReaderRepository) :
    ITicketingSession {

    private var calypsoPoIndex05h = 0
    private var calypsoPoIndex32h = 0
    private var navigoCardIndex05h = 0

    private var now = DateTime.now()

    private lateinit var calypsoPo: CalypsoPo

    private lateinit var cardSelection: CardSelectionsService

    override var poTypeName: String? = null
        private set

    override val poReader: Reader?
        get() = readerRepository.poReader

    override val samReader: Reader?
        get() = readerRepository.getSamReader()

    var poStructure: StructureEnum? = null
        private set

    private val allowedStructures: EnumMap<StructureEnum, List<String>> =
        EnumMap(StructureEnum::class.java)

    init {
        allowedStructures[StructureEnum.STRUCTURE_05H] =
            listOf(
                PO_TYPE_NAME_CALYPSO_05h,
                PO_TYPE_NAME_NAVIGO_05h
            )
        allowedStructures[StructureEnum.STRUCTURE_32H] =
            listOf(
                PO_TYPE_NAME_CALYPSO_32h
            )

        prepareAndSetPoDefaultSelection()
    }

    /**
     * prepare the default selection
     */
    override fun prepareAndSetPoDefaultSelection() {
        /*
         * Prepare a PO selection
         */
        cardSelection = CardSelectionsService(MultiSelectionProcessing.FIRST_MATCH)

        /* Select Calypso */
        val poSelectionRequest05h = PoSelection(
            PoSelector.builder()
                .cardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)
                .aidSelector(
                    CardSelector.AidSelector.builder()
                        .aidToSelect(AID_HIS_STRUCTURE_5H).build()
                )
                .invalidatedPo(PoSelector.InvalidatedPo.REJECT).build()
        )

        /*
         * Add the selection case to the current selection
         */
        calypsoPoIndex05h = cardSelection.prepareSelection(poSelectionRequest05h)

        val poSelectionRequest32h = PoSelection(
            PoSelector.builder()
                .cardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)
                .aidSelector(
                    CardSelector.AidSelector.builder()
                        .aidToSelect(AID_HIS_STRUCTURE_32H).build()
                )
                .invalidatedPo(PoSelector.InvalidatedPo.REJECT).build()
        )

        /*
         * Add the selection case to the current selection
         */
        calypsoPoIndex32h = cardSelection.prepareSelection(poSelectionRequest32h)

        /*
         * NAVIGO
         */
        val navigoCardSelectionRequest = PoSelection(
            PoSelector.builder()
                .cardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)
                .aidSelector(
                    CardSelector.AidSelector.builder()
                        .aidToSelect(AID_NORMALIZED_IDF_05H).build()
                )
                .invalidatedPo(PoSelector.InvalidatedPo.REJECT).build()
        )
        navigoCardIndex05h = cardSelection.prepareSelection(navigoCardSelectionRequest)

        /*
         * Provide the Reader with the selection operation to be processed when a PO is inserted.
         */
        (poReader as ObservableReader).setDefaultSelectionRequest(
            cardSelection.defaultSelectionsRequest, ObservableReader.NotificationMode.ALWAYS
        )
    }

    override fun processDefaultSelection(selectionResponse: AbstractDefaultSelectionsResponse?): CardSelectionsResult {
        Timber.i("selectionResponse = $selectionResponse")
        val selectionsResult: CardSelectionsResult =
            cardSelection.processDefaultSelectionsResponse(selectionResponse)
        if (selectionsResult.hasActiveSelection()) {
            when (selectionsResult.smartCards.keys.first()) {
                calypsoPoIndex05h -> {
                    calypsoPo = selectionsResult.activeSmartCard as CalypsoPo
                    poTypeName = PO_TYPE_NAME_CALYPSO_05h
                    poStructure = StructureEnum.findEnumByKey(calypsoPo.applicationSubtype.toInt())
                }
                calypsoPoIndex32h -> {
                    calypsoPo = selectionsResult.activeSmartCard as CalypsoPo
                    poTypeName = PO_TYPE_NAME_CALYPSO_32h
                    poStructure = StructureEnum.findEnumByKey(calypsoPo.applicationSubtype.toInt())
                }
                navigoCardIndex05h -> {
                    calypsoPo = selectionsResult.activeSmartCard as CalypsoPo
                    poTypeName = PO_TYPE_NAME_NAVIGO_05h
                    poStructure = StructureEnum.findEnumByKey(calypsoPo.applicationSubtype.toInt())
                }
                else -> poTypeName = PO_TYPE_NAME_OTHER
            }
        }

        Timber.i("PO type = $poTypeName")
        return selectionsResult
    }

    /**
     * initial PO content analysis
     *
     * @return
     */
    override fun checkStartupInfo(): Boolean = calypsoPo.startupInfo != null

    /**
     * Check card Structure
     */
    override fun checkStructure(): Boolean {
        if (!allowedStructures.containsKey(poStructure)) {
            return false
        }
        if (!allowedStructures[poStructure]!!.contains(poTypeName)) {
            return false
        }
        return true
    }

    /**
     * Launch the control procedure of the current PO
     *
     * @return [CardReaderResponse]
     */
    @Throws(
        CalypsoPoTransactionException::class,
        CalypsoPoCommandException::class,
        CalypsoSamCommandException::class,
        ControlException::class
    )
    override fun launchControlProcedure(locations: List<Location>): CardReaderResponse {
        return ControlProcedure().launch(
            calypsoPo = calypsoPo,
            samReader = samReader,
            ticketingSession = this@TicketingSession,
            locations = locations,
            now = now
        )
    }


    @Throws(KeypleReaderException::class, IllegalStateException::class)
    override fun checkSamAndOpenChannel(samReader: Reader): CardResource<CalypsoSam> {
        /*
         * check the availability of the SAM doing a ATR based selection, open its physical and
         * logical channels and keep it open
         */
        val samSelection = CardSelectionsService(MultiSelectionProcessing.FIRST_MATCH)

        val samSelector = SamSelector.builder()
            .cardProtocol(readerRepository.getSamReaderProtocol())
            .samRevision(SamRevision.C1)
            .build()

        samSelection.prepareSelection(SamSelection(samSelector))

        return try {
            if (samReader.isCardPresent) {
                val selectionResult = samSelection.processExplicitSelections(samReader)
                if (selectionResult.hasActiveSelection()) {
                    val calypsoSam = selectionResult.activeSmartCard as CalypsoSam
                    CardResource(samReader, calypsoSam)
                } else {
                    throw IllegalStateException("Sam selection failed")
                }
            } else {
                throw IllegalStateException("Sam is not present in the reader")
            }
        } catch (e: KeypleReaderException) {
            throw IllegalStateException("Reader exception: " + e.message)
        }
    }

    override fun getSecuritySettings(samResource: CardResource<CalypsoSam>?): PoSecuritySettings? {

        // The default KIF values for personalization, loading and debiting
        val DEFAULT_KIF_PERSO = 0x21.toByte()
        val DEFAULT_KIF_LOAD = 0x27.toByte()
        val DEFAULT_KIF_DEBIT = 0x30.toByte()
        // The default key record number values for personalization, loading and debiting
        // The actual value should be adjusted.
        val DEFAULT_KEY_RECORD_NUMBER_PERSO = 0x01.toByte()
        val DEFAULT_KEY_RECORD_NUMBER_LOAD = 0x02.toByte()
        val DEFAULT_KEY_RECORD_NUMBER_DEBIT = 0x03.toByte()

        /* define the security parameters to provide when creating PoTransaction */
        return PoSecuritySettings.PoSecuritySettingsBuilder(samResource) //
            .sessionDefaultKif(
                PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_PERSO,
                DEFAULT_KIF_PERSO
            ) //
            .sessionDefaultKif(
                PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_LOAD,
                DEFAULT_KIF_LOAD
            ) //
            .sessionDefaultKif(
                PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_DEBIT,
                DEFAULT_KIF_DEBIT
            ) //
            .sessionDefaultKeyRecordNumber(
                PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_PERSO,
                DEFAULT_KEY_RECORD_NUMBER_PERSO
            ) //
            .sessionDefaultKeyRecordNumber(
                PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_LOAD,
                DEFAULT_KEY_RECORD_NUMBER_LOAD
            ) //
            .sessionDefaultKeyRecordNumber(
                PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_DEBIT,
                DEFAULT_KEY_RECORD_NUMBER_DEBIT
            )
            .build()
    }
}
