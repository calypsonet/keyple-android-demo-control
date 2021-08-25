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
package org.calypsonet.keyple.demo.control.ticketing

import org.calypsonet.keyple.demo.control.di.scopes.AppScoped
import org.calypsonet.keyple.demo.control.exception.ControlException
import org.calypsonet.keyple.demo.control.models.CardReaderResponse
import org.calypsonet.keyple.demo.control.models.Location
import org.calypsonet.keyple.demo.control.models.FileStructureEnum
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_1TIC_ICA_1
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_1TIC_ICA_3
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_NORMALIZED_IDF
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_OTHER
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.DEFAULT_KIF_DEBIT
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.DEFAULT_KIF_LOAD
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.DEFAULT_KIF_PERSO
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.SAM_PROFILE_NAME
import org.calypsonet.keyple.demo.control.ticketing.procedure.ControlProcedure
import org.calypsonet.terminal.calypso.WriteAccessLevel
import org.calypsonet.terminal.calypso.card.CalypsoCard
import org.calypsonet.terminal.calypso.sam.CalypsoSam
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting
import org.calypsonet.terminal.reader.ObservableCardReader
import org.calypsonet.terminal.reader.selection.CardSelectionManager
import org.calypsonet.terminal.reader.selection.CardSelectionResult
import org.calypsonet.terminal.reader.selection.ScheduledCardSelectionsResponse
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.service.resource.CardResourceProfileConfigurator
import org.eclipse.keyple.core.service.resource.CardResourceService
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider
import org.eclipse.keyple.core.service.resource.PluginsConfigurator
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi
import org.joda.time.DateTime
import timber.log.Timber
import java.util.EnumMap
import javax.inject.Inject

@AppScoped
class TicketingSession @Inject constructor(private val readerRepository: IReaderRepository) :
    ITicketingSession {

    private var calypsoCardIndexAid_1TIC_ICA_1 = 0
    private var calypsoCardIndexAid_1TIC_ICA_3 = 0
    private var calypsoCardIndexAid_IDF = 0

    private lateinit var calypsoCard: CalypsoCard
    private lateinit var cardSelectionManager: CardSelectionManager

    override var cardAid: String? = null
        private set

    override val cardReader: Reader?
        get() = readerRepository.cardReader

    override val samReader: Reader?
        get() = readerRepository.getSamReader()

    private var fileStructure: FileStructureEnum? = null

    private val allowedFileStructures: EnumMap<FileStructureEnum, List<String>> =
        EnumMap(FileStructureEnum::class.java)

    init {
        allowedFileStructures[FileStructureEnum.FILE_STRUCTURE_02H] =
            listOf(
                AID_1TIC_ICA_1
            )
        allowedFileStructures[FileStructureEnum.FILE_STRUCTURE_05H] =
            listOf(
                AID_1TIC_ICA_1,
                AID_NORMALIZED_IDF
            )
        allowedFileStructures[FileStructureEnum.FILE_STRUCTURE_32H] =
            listOf(
                AID_1TIC_ICA_3
            )

        prepareAndSetCardDefaultSelection()
    }

    /**
     * prepare the default selection
     */
    override fun prepareAndSetCardDefaultSelection() {
        /*
         * Prepare a Card selection
         */
        cardSelectionManager =
            SmartCardServiceProvider.getService().createCardSelectionManager()

        /* Calypso selection: configures a CardSelector with all the desired attributes to make the selection and read additional information afterwards */
        val calypsoCardExtensionProvider = CalypsoExtensionService.getInstance()

        val smartCardService = SmartCardServiceProvider.getService()
        smartCardService.checkCardExtension(calypsoCardExtensionProvider)

        /* Select Calypso */
        val cardSelectionRequest_1TIC_ICA_1 =
            calypsoCardExtensionProvider.createCardSelection()
        cardSelectionRequest_1TIC_ICA_1
            .filterByDfName(AID_1TIC_ICA_1)
            .filterByCardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)

        /*
         * Add the selection case to the current selection
         */
        calypsoCardIndexAid_1TIC_ICA_1 = cardSelectionManager.prepareSelection(cardSelectionRequest_1TIC_ICA_1)

        val cardSelectionRequest_1TIC_ICA_3 =
            calypsoCardExtensionProvider.createCardSelection()
        cardSelectionRequest_1TIC_ICA_3
            .filterByDfName(AID_1TIC_ICA_3)
            .filterByCardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)

        /*
         * Add the selection case to the current selection
         */
        calypsoCardIndexAid_1TIC_ICA_3 = cardSelectionManager.prepareSelection(cardSelectionRequest_1TIC_ICA_3)

        /*
         * NAVIGO
         */

        val cardSelectionRequest_AID_IDF =
            calypsoCardExtensionProvider.createCardSelection()
        cardSelectionRequest_AID_IDF
            .filterByDfName(AID_NORMALIZED_IDF)
            .filterByCardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)

        /*
         * Add the selection case to the current selection
         */
        calypsoCardIndexAid_IDF = cardSelectionManager.prepareSelection(cardSelectionRequest_AID_IDF)

        /*
        * Schedule the execution of the prepared card selection scenario as soon as a card is presented
        */
        cardSelectionManager.scheduleCardSelectionScenario(
            cardReader as ObservableReader,
            ObservableCardReader.DetectionMode.REPEATING,
            ObservableCardReader.NotificationMode.ALWAYS
        )
    }

    override fun processDefaultSelection(selectionResponse: ScheduledCardSelectionsResponse?): CardSelectionResult {
        Timber.i("selectionResponse = $selectionResponse")
        val selectionsResult: CardSelectionResult =
            cardSelectionManager.parseScheduledCardSelectionsResponse(selectionResponse)
        if (selectionsResult.activeSelectionIndex != -1) {
            when (selectionsResult.smartCards.keys.first()) {
                calypsoCardIndexAid_1TIC_ICA_1 -> {
                    calypsoCard = selectionsResult.activeSmartCard as CalypsoCard
                    fileStructure =
                        FileStructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
                    cardAid = AID_1TIC_ICA_1
                }
                calypsoCardIndexAid_1TIC_ICA_3 -> {
                    calypsoCard = selectionsResult.activeSmartCard as CalypsoCard
                    cardAid = AID_1TIC_ICA_3
                    fileStructure =
                        FileStructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
                }
                calypsoCardIndexAid_IDF -> {
                    calypsoCard = selectionsResult.activeSmartCard as CalypsoCard
                    cardAid = AID_NORMALIZED_IDF
                    fileStructure =
                        FileStructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
                }
                else -> cardAid = AID_OTHER
            }
        }

        Timber.i("Card AID = $cardAid")
        return selectionsResult
    }

    /**
     * initial Card content analysis
     *
     * @return
     */
    override fun checkStartupInfo(): Boolean = calypsoCard.startupInfoRawData != null

    /**
     * Check card Structure
     */
    override fun checkStructure(): Boolean {
        if (!allowedFileStructures.containsKey(fileStructure)) {
            return false
        }
        if (!allowedFileStructures[fileStructure]!!.contains(cardAid)) {
            return false
        }
        return true
    }

    /**
     * Launch the control procedure of the current Card
     *
     * @return [CardReaderResponse]
     */
    @Throws(
        ControlException::class
    )
    override fun launchControlProcedure(locations: List<Location>): CardReaderResponse {
        return ControlProcedure().launch(
            calypsoCard = calypsoCard,
            samReader = samReader,
            ticketingSession = this@TicketingSession,
            locations = locations,
            now = DateTime.now()
        )
    }

    override fun getPlugin(): Plugin = readerRepository.getPlugin()

    override fun getSecuritySettings(): CardSecuritySetting? {

        val samCardResourceExtension =
            CalypsoExtensionService.getInstance()

        samCardResourceExtension.createCardSecuritySetting()

        // Create security settings that reference the same SAM profile requested from the card resource
        // service and enable the multiple session mode.
        val samResource = CardResourceServiceProvider.getService()
            .getCardResource(SAM_PROFILE_NAME)

        return CalypsoExtensionService.getInstance()
            .createCardSecuritySetting()
            .setSamResource(samResource.reader, samResource.smartCard as CalypsoSam)
            .assignDefaultKif(
                WriteAccessLevel.PERSONALIZATION,
                DEFAULT_KIF_PERSO
            )
            .assignDefaultKif(
                WriteAccessLevel.LOAD,
                DEFAULT_KIF_LOAD
            ) //
            .assignDefaultKif(
                WriteAccessLevel.DEBIT,
                DEFAULT_KIF_DEBIT
            ) //
            .enableMultipleSession()
    }

    /**
     * Setup the [CardResourceService] to provide a Calypso SAM C1 resource when requested.
     *
     * @param samProfileName A string defining the SAM profile.
     * @throws IllegalStateException If the expected card resource is not found.
     */

    override fun setupCardResourceService(samProfileName: String?) {
        // Create a card resource extension expecting a SAM "C1".
        val samSelection = CalypsoExtensionService.getInstance()
            .createSamSelection()
            .filterByProductType(CalypsoSam.ProductType.SAM_C1)

        val samCardResourceExtension =
            CalypsoExtensionService.getInstance().createSamResourceProfileExtension(samSelection)

        // Get the service
        val cardResourceService = CardResourceServiceProvider.getService()

        val pluginAndReaderExceptionHandler = PluginAndReaderExceptionHandler()

        // Configure the card resource service:
        // - allocation mode is blocking with a 100 milliseconds cycle and a 10 seconds timeout.
        // - the readers are searched in the PC/SC plugin, the observation of the plugin (for the
        // connection/disconnection of readers) and of the readers (for the insertion/removal of cards)
        // is activated.
        // - two card resource profiles A and B are defined, each expecting a specific card
        // characterized by its power-on data and placed in a specific reader.
        // - the timeout for using the card's resources is set at 5 seconds.
        cardResourceService
            .configurator
            .withBlockingAllocationMode(100, 10000)
            .withPlugins(
                PluginsConfigurator.builder()
                    .addPluginWithMonitoring(
                        getPlugin(),
                        readerRepository.getReaderConfiguratorSpi(),
                        pluginAndReaderExceptionHandler,
                        pluginAndReaderExceptionHandler
                    )
                    .withUsageTimeout(5000)
                    .build()
            )
            .withCardResourceProfiles(
                CardResourceProfileConfigurator.builder(samProfileName, samCardResourceExtension)
                    .withReaderNameRegex(readerRepository.getSamRegex())
                    .build()
            )
            .configure()

        cardResourceService.start()

        // verify the resource availability
        val cardResource = cardResourceService.getCardResource(samProfileName)
            ?: throw IllegalStateException(
                java.lang.String.format(
                    "Unable to retrieve a SAM card resource for profile '%s' from reader '%s' in plugin '%s'",
                    samProfileName, readerRepository.getSamRegex(), getPlugin().name
                )
            )

        // release the resource
        cardResourceService.releaseCardResource(cardResource)
    }

    /** Class implementing the exception handler SPIs for plugin and reader monitoring.  */
    private class PluginAndReaderExceptionHandler :
        PluginObservationExceptionHandlerSpi, CardReaderObservationExceptionHandlerSpi {
        override fun onPluginObservationError(pluginName: String, e: Throwable) {
            Timber.e("An exception occurred while monitoring the plugin '${e.message}'.")
        }

        override fun onReaderObservationError(
            pluginName: String,
            readerName: String,
            e: Throwable
        ) {
            Timber.e("An exception occurred while monitoring the plugin '${e.message}'.")
        }
    }
}
