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
package org.calypsonet.keyple.demo.control.di

import android.app.Activity
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.reader.PoReaderProtocol
import org.calypsonet.keyple.plugin.flowbird.FlowbirdPlugin
import org.calypsonet.keyple.plugin.flowbird.FlowbirdPluginFactoryProvider
import org.calypsonet.keyple.plugin.flowbird.FlowbirdUiManager
import org.calypsonet.keyple.plugin.flowbird.contact.FlowbirdContactReader
import org.calypsonet.keyple.plugin.flowbird.contact.SamSlot
import org.calypsonet.keyple.plugin.flowbird.contactless.FlowbirdContactlessReader
import org.calypsonet.keyple.plugin.flowbird.contactless.FlowbirdSupportContactlessProtocols
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi
import timber.log.Timber

class FlowbirdReaderRepositoryImpl @Inject constructor(
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) :
    IReaderRepository {

    override var poReader: Reader? = null
    override var samReaders: MutableList<Reader> = mutableListOf()

    @Throws(KeyplePluginException::class)
    override fun registerPlugin(activity: Activity) {
        runBlocking {
            val pluginFactory = withContext(Dispatchers.IO) {

                /*
                 * Init files used to sounds and colors from assets
                 */
                val mediaFiles: List<String> =
                    listOf("1_default_en.xml", "success.mp3", "error.mp3")
                val situationFiles: List<String> = listOf("1_default_en.xml")
                val translationFiles: List<String> = listOf("0_default.xml")

                FlowbirdPluginFactoryProvider.getFactory(
                    activity = activity,
                    mediaFiles = mediaFiles,
                    situationFiles = situationFiles,
                    translationFiles = translationFiles
                )
            }

            val smartCardService = SmartCardServiceProvider.getService()
            smartCardService.registerPlugin(pluginFactory)
        }
    }

    override fun getPlugin(): Plugin =
        SmartCardServiceProvider.getService().getPlugin(FlowbirdPlugin.PLUGIN_NAME)

    @Throws(KeyplePluginException::class)
    override suspend fun initPoReader(): Reader {
        val flowbirdPlugin =
            SmartCardServiceProvider.getService().getPlugin(FlowbirdPlugin.PLUGIN_NAME)
        val poReader = flowbirdPlugin?.getReader(FlowbirdContactlessReader.READER_NAME)

        poReader?.let {

            it.activateProtocol(
                getContactlessIsoProtocol().readerProtocolName,
                getContactlessIsoProtocol().applicationProtocolName
            )

            this.poReader = poReader
        }

        (poReader as ObservableReader).setReaderObservationExceptionHandler(
            readerObservationExceptionHandler
        )

        return poReader
    }

    @Throws(KeyplePluginException::class)
    override suspend fun initSamReaders(): List<Reader> {
        val plugin = SmartCardServiceProvider.getService().getPlugin(FlowbirdPlugin.PLUGIN_NAME)
        samReaders = plugin?.readers?.filter {
            !it.isContactless
        }?.toMutableList() ?: mutableListOf()

        if (!getSamReaderProtocol().isNullOrEmpty()) {
            samReaders.forEach {
                it.activateProtocol(
                    getSamReaderProtocol(),
                    getSamReaderProtocol()
                )
            }
        }
        return samReaders
    }

    override fun getSamReader(): Reader? {
        return if (samReaders.isNotEmpty()) {
            val filteredByName = samReaders.filter {
                it.name == SAM_READER_1_NAME
            }

            return if (filteredByName.isNullOrEmpty()) {
                samReaders.first()
            } else {
                filteredByName.first()
            }
        } else {
            null
        }
    }

    override fun getContactlessIsoProtocol(): PoReaderProtocol {
        return PoReaderProtocol(
            FlowbirdSupportContactlessProtocols.ALL.key,
            FlowbirdSupportContactlessProtocols.ALL.key
        )
    }

    override fun getSamReaderProtocol(): String? = null

    override fun clear() {
        poReader?.deactivateProtocol(getContactlessIsoProtocol().readerProtocolName)

        if (!getSamReaderProtocol().isNullOrEmpty()) {
            samReaders.forEach {
                it.deactivateProtocol(
                    getSamReaderProtocol()
                )
            }
        }
    }

    override fun displayWaiting(): Boolean {
        FlowbirdUiManager.displayWaiting()
        return true
    }

    override fun displayResultSuccess(): Boolean {
        FlowbirdUiManager.displayResultSuccess()
        return true
    }

    override fun displayResultFailed(): Boolean {
        FlowbirdUiManager.displayResultFailed()
        return true
    }

    override fun getSamRegex(): String = SAM_READER_NAME_REGEX

    override fun getReaderConfiguratorSpi(): ReaderConfiguratorSpi = ReaderConfigurator()

    /**
     * Reader configurator used by the card resource service to setup the SAM reader with the required
     * settings.
     */
    internal class ReaderConfigurator : ReaderConfiguratorSpi {
        /** {@inheritDoc}  */
        override fun setupReader(reader: Reader) {
            // Configure the reader with parameters suitable for contactless operations.
            try {
                reader.getExtension(FlowbirdContactReader::class.java)
            } catch (e: Exception) {
                Timber.e(
                    "Exception raised while setting up the reader ${reader.name} : ${e.message}"
                )
            }
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    companion object {
        @Suppress("unused")
        val SAM_READER_1_NAME =
            "${FlowbirdContactReader.READER_NAME}_${(SamSlot.ONE.slotId)}"

        @Suppress("unused")
        val SAM_READER_2_NAME =
            "${FlowbirdContactReader.READER_NAME}_${(SamSlot.TWO.slotId)}"

        @Suppress("unused")
        val SAM_READER_3_NAME =
            "${FlowbirdContactReader.READER_NAME}_${(SamSlot.THREE.slotId)}"

        @Suppress("unused")
        val SAM_READER_4_NAME =
            "${FlowbirdContactReader.READER_NAME}_${(SamSlot.FOUR.slotId)}"

        const val SAM_READER_NAME_REGEX = ".*ContactReader_0"
    }
}
