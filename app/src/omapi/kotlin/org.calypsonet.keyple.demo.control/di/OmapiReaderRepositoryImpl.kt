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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.reader.PoReaderProtocol
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi
import org.eclipse.keyple.core.util.protocol.ContactlessCardCommonProtocol
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPluginFactoryProvider
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiReader
import timber.log.Timber

class OmapiReaderRepositoryImpl @Inject constructor(
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) :
    IReaderRepository {

    lateinit var successMedia: MediaPlayer
    lateinit var errorMedia: MediaPlayer

    override var poReader: Reader? = null
    override var samReaders: MutableList<Reader> = mutableListOf()

    @Throws(KeyplePluginException::class)
    override fun registerPlugin(activity: Activity) {

        successMedia = MediaPlayer.create(activity, R.raw.success)
        errorMedia = MediaPlayer.create(activity, R.raw.error)

        val smartCardService = SmartCardServiceProvider.getService()
        try {

            val nfcPluginFactory = AndroidNfcPluginFactoryProvider(activity).getFactory()
            smartCardService.registerPlugin(nfcPluginFactory)

            AndroidOmapiPluginFactoryProvider(activity) {
                SmartCardServiceProvider.getService().registerPlugin(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getPlugin(): Plugin = SmartCardServiceProvider.getService().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)

    override fun getSamRegex(): String = ""

    @Throws(KeyplePluginException::class)
    override suspend fun initPoReader(): Reader? {
        val readerPlugin = SmartCardServiceProvider.getService().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)
        poReader = readerPlugin.getReader(AndroidNfcReader.READER_NAME)

        poReader?.let {

            // with this protocol settings we activate the nfc for ISO1443_4 protocol
            it.activateProtocol(
                getContactlessIsoProtocol().readerProtocolName,
                getContactlessIsoProtocol().applicationProtocolName
            )
        }

        (poReader as ObservableReader).setReaderObservationExceptionHandler(
            readerObservationExceptionHandler
        )

        return poReader
    }

    @Throws(KeyplePluginException::class)
    override suspend fun initSamReaders(): List<Reader> {
        /*
         * Wait until OMAPI sam readers are available.
         * If we do not wait, no retries are made after calling 'SmartCardService.getInstance().getPlugin(PLUGIN_NAME).readers'
         * -> then no reader is returned
         */
        @Suppress("BlockingMethodInNonBlockingContext")
        runBlocking {
            delay(250)
        }
        for (x in 1..MAX_TRIES) {
            val readerPlugin = SmartCardServiceProvider.getService().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)
            samReaders = readerPlugin.readers.toMutableList()
            if (samReaders.isEmpty()) {
                Timber.d("No readers found in OMAPI Keyple Plugin")
                Timber.d("Retrying in 1 second")
                delay(1000)
            } else {
                Timber.d("Readers Found")
                break
            }
        }
        samReaders.forEach {
            if(getSamReaderProtocol()?.isNotEmpty() == true){
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
                it.name == AndroidOmapiReader.READER_NAME_SIM_1
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
            ContactlessCardCommonProtocol.ISO_14443_4.name,
            ContactlessCardCommonProtocol.ISO_14443_4.name
        )
    }

    override fun getSamReaderProtocol(): String? = null

    override fun clear() {
        if(getSamReaderProtocol()?.isNotEmpty() == true){
            samReaders.forEach {
                it.deactivateProtocol(getSamReaderProtocol())
            }
        }

        poReader?.deactivateProtocol(getContactlessIsoProtocol().readerProtocolName)

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

    override fun getReaderConfiguratorSpi(): ReaderConfiguratorSpi {
        return ReaderConfigurator()
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
                reader.getExtension(AndroidOmapiReader::class.java)
            } catch (e: Exception) {
                Timber.e(
                    "Exception raised while setting up the reader ${reader.name} : ${e.message}"
                )
            }
        }
    }

    companion object {
        private const val MAX_TRIES = 10
    }
}
