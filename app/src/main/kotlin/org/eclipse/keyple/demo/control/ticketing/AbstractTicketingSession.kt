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

import org.eclipse.keyple.card.calypso.CalypsoCardExtension
import org.eclipse.keyple.card.calypso.po.ElementaryFile
import org.eclipse.keyple.card.calypso.po.PoSmartCard
import org.eclipse.keyple.card.calypso.transaction.PoSecuritySetting
import org.eclipse.keyple.card.calypso.transaction.PoTransactionService
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.selection.CardSelectionResult
import org.eclipse.keyple.core.service.selection.CardSelectionService
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

    protected lateinit var calypsoPo: PoSmartCard
    protected lateinit var cardSelection: CardSelectionService

    lateinit var calypsoCardExtensionProvider: CalypsoCardExtension

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

//    fun processSelectionsResult(selectionsResult: CardSelectionResult) {
//        val selectionIndex = selectionsResult.smartCards.keys.first()
//
//        if (selectionIndex == calypsoPoIndex) {
//            calypsoPo = selectionsResult.activeSmartCard as PoSmartCard
//            poTypeName = "CALYPSO"
//            efEnvironmentHolder = calypsoPo.getFileBySfi(CalypsoInfo.SFI_EnvironmentAndHolder)
//            efEventLog = calypsoPo.getFileBySfi(CalypsoInfo.SFI_EventLog)
//            efCounter = calypsoPo.getFileBySfi(CalypsoInfo.SFI_Counter)
//            efContractParser = calypsoPo.getFileBySfi(CalypsoInfo.SFI_Contracts)
//            efContractListParser = calypsoPo.getFileBySfi(CalypsoInfo.SFI_ContractList)
//        } else {
//            poTypeName = "OTHER"
//        }
//        Timber.i("PO type = $poTypeName")
//    }

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

//    @Throws(KeypleReaderException::class, IllegalStateException::class)
//    fun checkSamAndOpenChannel(samReader: Reader): CardResource<CalypsoSam> {
//        /*
//         * check the availability of the SAM doing a ATR based selection, open its physical and
//         * logical channels and keep it open
//         */
//        val samSelection = CardSelectionsService(MultiSelectionProcessing.FIRST_MATCH)
//
//        val samSelector = SamSelector.builder()
//            .cardProtocol(readerRepository.getSamReaderProtocol())
//            .samRevision(SamRevision.C1)
//            .build()
//
//        samSelection.prepareSelection(SamSelection(samSelector))
//
//        return try {
//            if (samReader.isCardPresent) {
//                val selectionResult = samSelection.processExplicitSelections(samReader)
//                if (selectionResult.hasActiveSelection()) {
//                    val calypsoSam = selectionResult.activeSmartCard as CalypsoSam
//                    CardResource(samReader, calypsoSam)
//                } else {
//                    throw IllegalStateException("Sam selection failed")
//                }
//            } else {
//                throw IllegalStateException("Sam is not present in the reader")
//            }
//        } catch (e: KeypleReaderException) {
//            throw IllegalStateException("Reader exception: " + e.message)
//        }
//    }

    fun getSecuritySettings(): PoSecuritySetting? {

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
        return PoSecuritySetting.builder("samResource") //
            .assignKif(
                PoTransactionService.SessionAccessLevel.SESSION_LVL_PERSO,
                DEFAULT_KIF_PERSO
            ) //
            .assignKif(
                PoTransactionService.SessionAccessLevel.SESSION_LVL_LOAD,
                DEFAULT_KIF_LOAD
            ) //
            .assignKif(
                PoTransactionService.SessionAccessLevel.SESSION_LVL_DEBIT,
                DEFAULT_KIF_DEBIT
            ) //
            .assignKeyRecordNumber(
                PoTransactionService.SessionAccessLevel.SESSION_LVL_PERSO,
                DEFAULT_KEY_RECORD_NUMBER_PERSO
            ) //
            .assignKeyRecordNumber(
                PoTransactionService.SessionAccessLevel.SESSION_LVL_LOAD,
                DEFAULT_KEY_RECORD_NUMBER_LOAD
            ) //
            .assignKeyRecordNumber(
                PoTransactionService.SessionAccessLevel.SESSION_LVL_DEBIT,
                DEFAULT_KEY_RECORD_NUMBER_DEBIT
            )
            .build()
    }

    fun parseCardlet(cardletDto: CardletInputDto): CardletDto? = CardletParser().parseCardlet(cardletDto)
}

