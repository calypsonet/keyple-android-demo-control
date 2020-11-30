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

import org.eclipse.keyple.calypso.command.sam.SamRevision
import org.eclipse.keyple.calypso.transaction.CalypsoPo
import org.eclipse.keyple.calypso.transaction.CalypsoSam
import org.eclipse.keyple.calypso.transaction.ElementaryFile
import org.eclipse.keyple.calypso.transaction.PoSecuritySettings
import org.eclipse.keyple.calypso.transaction.PoSecuritySettings.PoSecuritySettingsBuilder
import org.eclipse.keyple.calypso.transaction.PoTransaction.SessionSetting.AccessLevel
import org.eclipse.keyple.calypso.transaction.SamSelection
import org.eclipse.keyple.calypso.transaction.SamSelector
import org.eclipse.keyple.core.card.selection.CardResource
import org.eclipse.keyple.core.card.selection.CardSelectionsResult
import org.eclipse.keyple.core.card.selection.CardSelectionsService
import org.eclipse.keyple.core.card.selection.MultiSelectionProcessing
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.event.ObservableReader
import org.eclipse.keyple.core.service.exception.KeypleReaderException
import org.eclipse.keyple.demo.control.di.scopes.AppScoped
import org.eclipse.keyple.demo.control.reader.IReaderRepository
import org.eclipse.keyple.parser.dto.CardletInputDto
import org.eclipse.keyple.parser.keyple.CardletParser
import org.eclipse.keyple.parser.model.CardletDto
import timber.log.Timber

@AppScoped
abstract class AbstractTicketingSession protected constructor(
    protected val readerRepository: IReaderRepository
) {

    protected lateinit var calypsoPo: CalypsoPo
    protected lateinit var cardSelection: CardSelectionsService
    var poTypeName: String? = null
        protected set
    var cardContent: CardContent = CardContent()
        protected set
    protected var currentPoSN: ByteArray? = null

    protected var calypsoPoIndex = 0

    protected lateinit var efEnvironmentHolder: ElementaryFile
    protected lateinit var efEventLog: ElementaryFile
    protected lateinit var efCounter: ElementaryFile
    protected lateinit var efContractParser: ElementaryFile
    protected lateinit var efContractListParser: ElementaryFile


    val poReader: Reader?
        get() = readerRepository.poReader

    protected fun pad(text: String, c: Char, length: Int): String {
        val sb = StringBuffer(length)
        sb.append(text)
        for (i in text.length until length) {
            sb.append(c)
        }
        return sb.toString()
    }

    fun processSelectionsResult(selectionsResult: CardSelectionsResult) {
        val selectionIndex = selectionsResult.smartCards.keys.first()

        if (selectionIndex == calypsoPoIndex) {
            calypsoPo = selectionsResult.activeSmartCard as CalypsoPo
            poTypeName = "CALYPSO"
            efEnvironmentHolder = calypsoPo.getFileBySfi(CalypsoInfo.SFI_EnvironmentAndHolder)
            efEventLog = calypsoPo.getFileBySfi(CalypsoInfo.SFI_EventLog)
            efCounter = calypsoPo.getFileBySfi(CalypsoInfo.SFI_Counter)
            efContractParser = calypsoPo.getFileBySfi(CalypsoInfo.SFI_Contracts)
            efContractListParser = calypsoPo.getFileBySfi(CalypsoInfo.SFI_ContractList)
        } else {
            poTypeName = "OTHER"
        }
        Timber.i("PO type = $poTypeName")
    }

    val poIdentification: String
        get() = (calypsoPo.applicationSerialNumber + ", " +
                calypsoPo.revision.toString())

    /**
     * initial PO content analysis
     *
     * @return
     */
    fun analyzePoProfile(): Boolean {
        var status = false
        if (calypsoPo.startupInfo != null) {
            currentPoSN = calypsoPo.applicationSerialNumberBytes
            cardContent.serialNumber = currentPoSN
            cardContent.poRevision = calypsoPo.revision.toString()
            status = true
        }

        return status
    }

    fun notifySeProcessed() {
        (readerRepository.poReader as ObservableReader).finalizeCardProcessing()
    }

    @Throws(KeypleReaderException::class, IllegalStateException::class)
    fun checkSamAndOpenChannel(samReader: Reader): CardResource<CalypsoSam> {
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

    open fun getSecuritySettings(samResource: CardResource<CalypsoSam>?): PoSecuritySettings? {

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
        return PoSecuritySettingsBuilder(samResource) //
            .sessionDefaultKif(AccessLevel.SESSION_LVL_PERSO, DEFAULT_KIF_PERSO) //
            .sessionDefaultKif(AccessLevel.SESSION_LVL_LOAD, DEFAULT_KIF_LOAD) //
            .sessionDefaultKif(AccessLevel.SESSION_LVL_DEBIT, DEFAULT_KIF_DEBIT) //
            .sessionDefaultKeyRecordNumber(
                AccessLevel.SESSION_LVL_PERSO,
                DEFAULT_KEY_RECORD_NUMBER_PERSO
            ) //
            .sessionDefaultKeyRecordNumber(
                AccessLevel.SESSION_LVL_LOAD,
                DEFAULT_KEY_RECORD_NUMBER_LOAD
            ) //
            .sessionDefaultKeyRecordNumber(
                AccessLevel.SESSION_LVL_DEBIT,
                DEFAULT_KEY_RECORD_NUMBER_DEBIT
            )
            .build()
    }

    fun parseCardlet(cardletDto: CardletInputDto): CardletDto? = CardletParser().parseCardlet(cardletDto)
}

