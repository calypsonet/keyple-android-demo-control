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
import android.media.MediaPlayer
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
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPlugin
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPluginFactoryProvider
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoReader
import org.eclipse.keyple.famoco.se.plugin.utils.ContactCardCommonProtocols
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import timber.log.Timber
import javax.inject.Inject

class FamocoReaderRepositoryImpl @Inject constructor(private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi) :
    IReaderRepository {

    lateinit var successMedia: MediaPlayer
    lateinit var errorMedia: MediaPlayer

    override var poReader: Reader? = null
    override var samReaders: MutableList<Reader> = mutableListOf()

    @Throws(KeyplePluginException::class)
    override fun registerPlugin(activity: Activity) {

        successMedia = MediaPlayer.create(activity, R.raw.success)
        errorMedia = MediaPlayer.create(activity, R.raw.error)

        try {
            val androidNfcPluginFactory = AndroidNfcPluginFactoryProvider(activity).getFactory()
            SmartCardServiceProvider.getService().registerPlugin(androidNfcPluginFactory)

            val androidFamocoPluginFactory = AndroidFamocoPluginFactoryProvider.getFactory()
            SmartCardServiceProvider.getService().registerPlugin(androidFamocoPluginFactory)
        } catch (e: UnsatisfiedLinkError) {
            Timber.w(e)
        }
    }

    override fun getPlugin(): Plugin =
        SmartCardServiceProvider.getService().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME)

    @Throws(KeyplePluginException::class)
    override suspend fun initPoReader(): Reader {
        val readerPlugin =
            SmartCardServiceProvider.getService().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)
        val poReader = readerPlugin.getReader(AndroidNfcReader.READER_NAME)

        poReader?.let {
            // with this protocol settings we activate the nfc for ISO1443_4 protocol
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

    override suspend fun initSamReaders(): List<Reader> {
        if (samReaders.isNullOrEmpty()) {
            val samPlugin =
                SmartCardServiceProvider.getService().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME)

            if (samPlugin != null) {
                val samReader = samPlugin.getReader(AndroidFamocoReader.READER_NAME)
                samReader?.let {
                    it.activateProtocol(
                        getSamReaderProtocol(),
                        getSamReaderProtocol()
                    )

                    samReaders.add(it)
                }
            }
        }

        return samReaders
    }

    override fun getSamReader(): Reader? {
        return if (samReaders.isNotEmpty()) {
            val filteredByName = samReaders.filter {
                it.name == AndroidFamocoReader.READER_NAME
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

    override fun getSamRegex(): String = SAM_READER_NAME_REGEX

    override fun getContactlessIsoProtocol(): PoReaderProtocol {
        return PoReaderProtocol(
            ContactlessCardCommonProtocol.ISO_14443_4.name,
            ContactlessCardCommonProtocol.ISO_14443_4.name
        )
    }

    override fun getSamReaderProtocol(): String {
        return ContactCardCommonProtocols.ISO_7816_3.name
    }

    override fun clear() {
        poReader?.deactivateProtocol(getContactlessIsoProtocol().readerProtocolName)

        val samReader = getSamReader()
        samReader?.deactivateProtocol(getSamReaderProtocol())

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
                reader.getExtension(AndroidFamocoReader::class.java)
            } catch (e: Exception) {
                Timber.e(
                    "Exception raised while setting up the reader ${reader.name} : ${e.message}"
                )
            }
        }
    }

    companion object {
        const val SAM_READER_NAME_REGEX = ".*FamocoReader"
    }
}
