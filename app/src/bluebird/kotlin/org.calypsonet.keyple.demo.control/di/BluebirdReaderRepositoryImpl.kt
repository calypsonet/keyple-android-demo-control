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
import android.media.MediaPlayer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.reader.CardReaderProtocol
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactReader
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactlessReader
import org.calypsonet.keyple.plugin.bluebird.BluebirdPlugin
import org.calypsonet.keyple.plugin.bluebird.BluebirdPluginFactoryProvider
import org.calypsonet.keyple.plugin.bluebird.BluebirdSupportContactlessProtocols
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi
import org.eclipse.keyple.core.util.protocol.ContactCardCommonProtocol
import timber.log.Timber

class BluebirdReaderRepositoryImpl @Inject constructor(
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) :
    IReaderRepository {

    private lateinit var successMedia: MediaPlayer
    private lateinit var errorMedia: MediaPlayer

    override var cardReader: Reader? = null
    override var samReaders: MutableList<Reader> = mutableListOf()

    @Throws(KeyplePluginException::class)
    override fun registerPlugin(activity: Activity) {
        runBlocking {

            successMedia = MediaPlayer.create(activity, R.raw.success)
            errorMedia = MediaPlayer.create(activity, R.raw.error)

            val pluginFactory = withContext(Dispatchers.IO) {
                BluebirdPluginFactoryProvider.getFactory(activity)
            }
            val smartCardService = SmartCardServiceProvider.getService()
            smartCardService.registerPlugin(pluginFactory)
        }
    }

    override fun getPlugin(): Plugin = SmartCardServiceProvider.getService().getPlugin(BluebirdPlugin.PLUGIN_NAME)

    @Throws(KeyplePluginException::class)
    override suspend fun initPoReader(): Reader {
        val bluebirdPlugin =
            SmartCardServiceProvider.getService().getPlugin(BluebirdPlugin.PLUGIN_NAME)
        val poReader = bluebirdPlugin?.getReader(BluebirdContactlessReader.READER_NAME)
        poReader?.let {

            it.activateProtocol(
                getContactlessIsoProtocol().readerProtocolName,
                getContactlessIsoProtocol().applicationProtocolName
            )

            this.cardReader = poReader
        }

        (poReader as ObservableReader).setReaderObservationExceptionHandler(
            readerObservationExceptionHandler
        )

        return poReader
    }

    @Throws(KeyplePluginException::class)
    override suspend fun initSamReaders(): List<Reader> {
        val bluebirdPlugin =
            SmartCardServiceProvider.getService().getPlugin(BluebirdPlugin.PLUGIN_NAME)
        samReaders = bluebirdPlugin?.readers?.filter {
            !it.isContactless
        }?.toMutableList() ?: mutableListOf()

        samReaders.forEach { reader ->
            reader.activateProtocol(
                getSamReaderProtocol(),
                getSamReaderProtocol()
            )
        }
        return samReaders
    }

    override fun getSamReader(): Reader? {
        return if (samReaders.isNotEmpty()) {
            val filteredByName = samReaders.filter {
                it.name == BluebirdContactReader.READER_NAME
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

    override fun getContactlessIsoProtocol(): CardReaderProtocol {
        return CardReaderProtocol(
            BluebirdSupportContactlessProtocols.NFC_ALL.key,
            BluebirdSupportContactlessProtocols.NFC_ALL.key
        )
    }

    override fun getSamReaderProtocol(): String =
        ContactCardCommonProtocol.ISO_7816_3.name

    override fun getSamRegex(): String = SAM_READER_NAME_REGEX

    override fun clear() {
        cardReader?.deactivateProtocol(getContactlessIsoProtocol().readerProtocolName)

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

    override fun getPermissions(): Array<String> = arrayOf(BluebirdPlugin.BLUEBIRD_SAM_PERMISSION)

    override fun displayResultSuccess(): Boolean {
        successMedia.start()
        return true
    }

    override fun displayResultFailed(): Boolean {
        errorMedia.start()
        return true
    }

    companion object {
        const val SAM_READER_NAME_REGEX = ".*ContactReader"
    }

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
                reader.getExtension(BluebirdContactReader::class.java)
            } catch (e: Exception) {
                Timber.e(
                    "Exception raised while setting up the reader ${reader.name} : ${e.message}"
                )
            }
        }
    }
}
