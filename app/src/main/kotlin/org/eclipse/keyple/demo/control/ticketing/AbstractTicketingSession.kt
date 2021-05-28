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

import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.card.calypso.CalypsoExtensionServiceProvider
import org.eclipse.keyple.card.calypso.card.CalypsoCard
import org.eclipse.keyple.card.calypso.card.ElementaryFile
import org.eclipse.keyple.card.calypso.sam.SamRevision
import org.eclipse.keyple.card.calypso.transaction.CardSecuritySetting
import org.eclipse.keyple.card.calypso.transaction.CardTransactionService
import org.eclipse.keyple.core.common.KeypleReaderExtension
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.resource.CardResourceProfileConfigurator
import org.eclipse.keyple.core.service.resource.CardResourceService
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider
import org.eclipse.keyple.core.service.resource.PluginsConfigurator
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi
import org.eclipse.keyple.core.service.selection.CardSelectionService
import org.eclipse.keyple.demo.control.di.scopes.AppScoped
import org.eclipse.keyple.demo.control.reader.IReaderRepository
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo.SAM_PROFILE_NAME
import org.eclipse.keyple.parser.dto.CardletInputDto
import org.eclipse.keyple.parser.keyple.CardletParser
import org.eclipse.keyple.parser.model.CardletDto
import timber.log.Timber

@AppScoped
abstract class AbstractTicketingSession protected constructor(
    protected val readerRepository: IReaderRepository
) {

    protected lateinit var calypsoCard: CalypsoCard
    protected lateinit var cardSelection: CardSelectionService

    lateinit var calypsoCardExtensionProvider: CalypsoExtensionService

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
        get() = (calypsoCard.applicationSerialNumber + ", " +
                calypsoCard.revision.toString())

    /**
     * initial PO content analysis
     *
     * @return
     */
    fun analyzePoProfile(): Boolean {
        var status = false
        if (calypsoCard.startupInfo != null) {
            currentPoSN = calypsoCard.applicationSerialNumberBytes
            cardContent.serialNumber = currentPoSN
            cardContent.poRevision = calypsoCard.revision.toString()
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

    fun getSecuritySettings(): CardSecuritySetting? {

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
        return CardSecuritySetting.builder() //
            .setSamCardResourceProfileName(SAM_PROFILE_NAME)
            .assignKif(
                CardTransactionService.SessionAccessLevel.SESSION_LVL_PERSO,
                DEFAULT_KIF_PERSO
            ) //
            .assignKif(
                CardTransactionService.SessionAccessLevel.SESSION_LVL_LOAD,
                DEFAULT_KIF_LOAD
            ) //
            .assignKif(
                CardTransactionService.SessionAccessLevel.SESSION_LVL_DEBIT,
                DEFAULT_KIF_DEBIT
            ) //
            .assignKeyRecordNumber(
                CardTransactionService.SessionAccessLevel.SESSION_LVL_PERSO,
                DEFAULT_KEY_RECORD_NUMBER_PERSO
            ) //
            .assignKeyRecordNumber(
                CardTransactionService.SessionAccessLevel.SESSION_LVL_LOAD,
                DEFAULT_KEY_RECORD_NUMBER_LOAD
            ) //
            .assignKeyRecordNumber(
                CardTransactionService.SessionAccessLevel.SESSION_LVL_DEBIT,
                DEFAULT_KEY_RECORD_NUMBER_DEBIT
            )
            .build()
    }

    fun parseCardlet(cardletDto: CardletInputDto): CardletDto? = CardletParser().parseCardlet(cardletDto)

    /**
     * Setup the [CardResourceService] to provide a Calypso SAM C1 resource when requested.
     *
     * @param plugin The plugin to which the SAM reader belongs.
     * @param readerNameRegex A regular expression matching the expected SAM reader name.
     * @param samProfileName A string defining the SAM profile.
     * @throws IllegalStateException If the expected card resource is not found.
     */
    fun setupCardResourceService(
        readerNameRegex: String?, samProfileName: String?
    ) {

        // Create a card resource extension expecting a SAM "C1".
        val samCardResourceExtension =
            CalypsoExtensionServiceProvider.getService()
                .createSamResourceProfileExtension()
                .setSamRevision(SamRevision.C1)

        val plugin = readerRepository.getPlugin()
        // Get the service
        val cardResourceService = CardResourceServiceProvider.getService()

        // Create a minimalist configuration (no plugin/reader observation)
        cardResourceService
            .configurator
            .withPlugins(
                PluginsConfigurator.builder().addPlugin(plugin, ReaderConfigurator(readerRepository)).build()
            )
            .withCardResourceProfiles(
                CardResourceProfileConfigurator.builder(samProfileName, samCardResourceExtension)
                    .withReaderNameRegex(readerNameRegex)
                    .build()
            )
            .configure()
        cardResourceService.start()

        // verify the resource availability
        val cardResource = cardResourceService.getCardResource(samProfileName)
            ?: throw IllegalStateException(
                java.lang.String.format(
                    "Unable to retrieve a SAM card resource for profile '%s' from reader '%s' in plugin '%s'",
                    samProfileName, readerNameRegex, plugin.name
                )
            )

        // release the resource
        cardResourceService.releaseCardResource(cardResource)
    }

    /**
     * Reader configurator used by the card resource service to setup the SAM reader with the required
     * settings.
     */
    private class ReaderConfigurator(val readerRepository: IReaderRepository) : ReaderConfiguratorSpi {
        /** {@inheritDoc}  */
        override fun setupReader(reader: Reader) {
            // Configure the reader with parameters suitable for contactless operations.
            try {
                reader
                    .getExtension((readerRepository.getSamReader() as KeypleReaderExtension)::class.java)
//                    .setContactless(false)
//                    .setIsoProtocol(PcscReader.IsoProtocol.T0)
//                    .setSharingMode(PcscReader.SharingMode.SHARED)
            } catch (e: Exception) {
                Timber.e(
                    "Exception raised while setting up the reader ${reader.getName()} : ${e.message}")
            }
        }
    }
}

