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

import org.calypsonet.keyple.demo.control.di.scopes.AppScoped
import org.calypsonet.keyple.demo.control.exception.ControlException
import org.calypsonet.keyple.demo.control.models.CardReaderResponse
import org.calypsonet.keyple.demo.control.models.Location
import org.calypsonet.keyple.demo.control.models.StructureEnum
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_HIS_STRUCTURE_32H
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_HIS_STRUCTURE_5H_2H
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.AID_NORMALIZED_IDF_05H
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.DEFAULT_KIF_DEBIT
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.DEFAULT_KIF_LOAD
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.DEFAULT_KIF_PERSO
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_CALYPSO_02h
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_CALYPSO_05h
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_CALYPSO_32h
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_NAVIGO_05h
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo.PO_TYPE_NAME_OTHER
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

    private var calypsoPoIndex05h_02h = 0
    private var calypsoPoIndex32h = 0
    private var navigoCardIndex05h = 0

    private var now = DateTime.now()

    private lateinit var calypsoCard: CalypsoCard

    private lateinit var cardSelectionManager: CardSelectionManager

    override var poTypeName: String? = null
        private set

    override val poReader: Reader?
        get() = readerRepository.poReader

    override val samReader: Reader?
        get() = readerRepository.getSamReader()

    private var poStructure: StructureEnum? = null

    private val allowedStructures: EnumMap<StructureEnum, List<String>> =
        EnumMap(StructureEnum::class.java)

    init {
        allowedStructures[StructureEnum.STRUCTURE_02H] =
            listOf(
                PO_TYPE_NAME_CALYPSO_02h
            )
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
        cardSelectionManager =
            SmartCardServiceProvider.getService().createCardSelectionManager()

        /* Calypso selection: configures a PoSelector with all the desired attributes to make the selection and read additional information afterwards */
        val calypsoCardExtensionProvider = CalypsoExtensionService.getInstance()

        val smartCardService = SmartCardServiceProvider.getService()
        smartCardService.checkCardExtension(calypsoCardExtensionProvider)

        /* Select Calypso */
        val poSelectionRequest05h_02h =
            calypsoCardExtensionProvider.createCardSelection()
        poSelectionRequest05h_02h
            .filterByDfName(AID_HIS_STRUCTURE_5H_2H)
            .filterByCardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)

        /*
         * Add the selection case to the current selection
         */
        calypsoPoIndex05h_02h = cardSelectionManager.prepareSelection(poSelectionRequest05h_02h)

        val poSelectionRequest32h =
            calypsoCardExtensionProvider.createCardSelection()
        poSelectionRequest32h
            .filterByDfName(AID_HIS_STRUCTURE_32H)
            .filterByCardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)

        /*
         * Add the selection case to the current selection
         */
        calypsoPoIndex32h = cardSelectionManager.prepareSelection(poSelectionRequest32h)

        /*
         * NAVIGO
         */

        val navigoCardSelectionRequest =
            calypsoCardExtensionProvider.createCardSelection()
        navigoCardSelectionRequest
            .filterByDfName(AID_NORMALIZED_IDF_05H)
            .filterByCardProtocol(readerRepository.getContactlessIsoProtocol()!!.applicationProtocolName)

        /*
         * Add the selection case to the current selection
         */
        navigoCardIndex05h = cardSelectionManager.prepareSelection(navigoCardSelectionRequest)

        /*
        * Schedule the execution of the prepared card selection scenario as soon as a card is presented
        */
        cardSelectionManager.scheduleCardSelectionScenario(
            poReader as ObservableReader,
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
                calypsoPoIndex05h_02h -> {
                    calypsoCard = selectionsResult.activeSmartCard as CalypsoCard
                    poStructure =
                        StructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
                    when(poStructure){
                        StructureEnum.STRUCTURE_02H -> poTypeName = PO_TYPE_NAME_CALYPSO_02h
                        StructureEnum.STRUCTURE_05H -> poTypeName = PO_TYPE_NAME_CALYPSO_05h
                        else -> {
                            //Do nothing
                        }
                    }
                }
                calypsoPoIndex32h -> {
                    calypsoCard = selectionsResult.activeSmartCard as CalypsoCard
                    poTypeName = PO_TYPE_NAME_CALYPSO_32h
                    poStructure =
                        StructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
                }
                navigoCardIndex05h -> {
                    calypsoCard = selectionsResult.activeSmartCard as CalypsoCard
                    poTypeName = PO_TYPE_NAME_NAVIGO_05h
                    poStructure =
                        StructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
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
    override fun checkStartupInfo(): Boolean = calypsoCard.startupInfoRawData != null

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
        ControlException::class
    )
    override fun launchControlProcedure(locations: List<Location>): CardReaderResponse {
        return ControlProcedure().launch(
            calypsoCard = calypsoCard,
            samReader = samReader,
            ticketingSession = this@TicketingSession,
            locations = locations,
            now = now
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
