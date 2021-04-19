/*
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

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
package org.calypsonet.keyple.demo.control.di

import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.reader.PoReaderProtocol
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keyple.coppernic.ask.plugin.Cone2ContactReader
import org.eclipse.keyple.coppernic.ask.plugin.Cone2ContactlessReader
import org.eclipse.keyple.coppernic.ask.plugin.Cone2Plugin
import org.eclipse.keyple.coppernic.ask.plugin.Cone2PluginFactory
import org.eclipse.keyple.coppernic.ask.plugin.Cone2PluginFactoryProvider
import org.eclipse.keyple.coppernic.ask.plugin.ParagonSupportedContactProtocols
import org.eclipse.keyple.coppernic.ask.plugin.ParagonSupportedContactlessProtocols
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi
import timber.log.Timber
import javax.inject.Inject

class CoppernicReaderRepositoryImpl @Inject constructor(private val applicationContext: Context, private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi) :
    IReaderRepository {

    lateinit var successMedia: MediaPlayer
    lateinit var errorMedia: MediaPlayer

    override var poReader: Reader? = null
    override var samReaders: MutableList<Reader> = mutableListOf()

    @Throws(KeyplePluginException::class)
    override fun registerPlugin(activity: Activity) {
        runBlocking {

            successMedia = MediaPlayer.create(activity, R.raw.success)
            errorMedia = MediaPlayer.create(activity, R.raw.error)

            val pluginFactory: Cone2PluginFactory?
            pluginFactory = withContext(Dispatchers.IO) {
                Cone2PluginFactoryProvider.getFactory(applicationContext)
            }

            SmartCardServiceProvider.getService().registerPlugin(pluginFactory)
        }
    }

    override fun getPlugin(): Plugin = SmartCardServiceProvider.getService().getPlugin(Cone2Plugin.PLUGIN_NAME)

    @Throws(KeyplePluginException::class)
    override suspend fun initPoReader(): Reader? {
        val askPlugin =
            SmartCardServiceProvider.getService().getPlugin(Cone2Plugin.PLUGIN_NAME)
        val poReader = askPlugin?.getReader(Cone2ContactlessReader.READER_NAME)
        poReader?.let {

            it.activateProtocol(
                getContactlessIsoProtocol().readerProtocolName,
                getContactlessIsoProtocol().applicationProtocolName
            )

            (poReader as ObservableReader).setReaderObservationExceptionHandler(
                readerObservationExceptionHandler
            )

            this.poReader = poReader
        }

        return poReader
    }

    @Throws(KeyplePluginException::class)
    override suspend fun initSamReaders(): List<Reader> {
        val askPlugin =
            SmartCardServiceProvider.getService().getPlugin(Cone2Plugin.PLUGIN_NAME)
        samReaders = askPlugin?.readers?.filter {
            !it.isContactless
        }?.toMutableList() ?: mutableListOf()

        samReaders.forEach {
            it.activateProtocol(
                getSamReaderProtocol(),
                getSamReaderProtocol()
            )
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
            ParagonSupportedContactlessProtocols.ISO_14443.name,
            ParagonSupportedContactlessProtocols.ISO_14443.name
        )
    }

    override fun getSamReaderProtocol(): String = ParagonSupportedContactProtocols.INNOVATRON_HIGH_SPEED_PROTOCOL.name

    override fun getSamRegex(): String = SAM_READER_NAME_REGEX

    override fun getReaderConfiguratorSpi(): ReaderConfiguratorSpi = ReaderConfigurator()

    override fun clear() {
        poReader?.deactivateProtocol(getContactlessIsoProtocol().readerProtocolName)

        samReaders.forEach {
            it.deactivateProtocol(
                getSamReaderProtocol()
            )
        }

        successMedia.stop()
        successMedia.release()

        errorMedia.stop()
        errorMedia.release()
    }

    override fun displayResultSuccess(): Boolean {
        successMedia.start()
        return true
    }

    override fun displayResultFailed(): Boolean {
        errorMedia.start()
        return true
    }

    companion object {
        private const val SAM_READER_SLOT_1 = "1"
        const val SAM_READER_1_NAME =
            "${Cone2ContactReader.READER_NAME}_$SAM_READER_SLOT_1"

        const val SAM_READER_NAME_REGEX = ".*ContactReader_1"
    }

    /**
     * Reader configurator used by the card resource service to setup the SAM reader with the required
     * settings.
     */
    internal class ReaderConfigurator : ReaderConfiguratorSpi {
        /** {@inheritDoc}  */
        override fun setupReader(reader: Reader) {
            // Configure the reader with parameters suitable for contactless operations.
            try {
                reader.getExtension(Cone2ContactReader::class.java)
            } catch (e: Exception) {
                Timber.e(
                    "Exception raised while setting up the reader ${reader.name} : ${e.message}"
                )
            }
        }
    }
}
