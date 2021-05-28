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
package org.eclipse.keyple.demo.control.di

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.eclipse.keyple.core.plugin.AbstractLocalReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardService
import org.eclipse.keyple.core.service.event.ObservableReader
import org.eclipse.keyple.core.service.event.ReaderObservationExceptionHandler
import org.eclipse.keyple.core.service.exception.KeypleException
import org.eclipse.keyple.core.service.exception.KeyplePluginInstantiationException
import org.eclipse.keyple.core.service.util.ContactCardCommonProtocols
import org.eclipse.keyple.core.service.util.ContactlessCardCommonProtocols
import org.eclipse.keyple.demo.control.reader.IReaderRepository
import org.eclipse.keyple.demo.control.reader.PoReaderProtocol
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactory
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcProtocolSettings
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPluginFactory
import org.eclipse.keyple.plugin.android.omapi.PLUGIN_NAME
import timber.log.Timber
import javax.inject.Inject

class OmapiReaderRepositoryImpl @Inject constructor(
    private val applicationContext: Context,
    private val readerObservationExceptionHandler: ReaderObservationExceptionHandler
) :
    IReaderRepository {

    override var poReader: Reader? = null
    override var samReaders: MutableMap<String, Reader> = mutableMapOf()

    @Throws(KeypleException::class)
    override fun registerPlugin(activity: Activity) {
        SmartCardService.getInstance().registerPlugin(
            AndroidNfcPluginFactory(
                activity,
                readerObservationExceptionHandler
            )
        )
        try {
            AndroidOmapiPluginFactory(applicationContext) {
                SmartCardService.getInstance().registerPlugin(it)
            }
        } catch (e: KeyplePluginInstantiationException) {
            e.printStackTrace()
        }
    }

    @Throws(KeypleException::class)
    override suspend fun initPoReader(): Reader? {
        val readerPlugin = SmartCardService.getInstance().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)
        poReader = readerPlugin.readers[AndroidNfcReader.READER_NAME]

        poReader?.let {
            val androidNfcReader = it as AndroidNfcReader
            Timber.d("Initialize SEProxy with Android Plugin")

            // define task as an observer for ReaderEvents
            Timber.d("PO (NFC) reader name: ${it.name}")

            androidNfcReader.presenceCheckDelay = 100
            androidNfcReader.noPlateformSound = false
            androidNfcReader.skipNdefCheck = false

            (poReader as ObservableReader).activateProtocol(
                getContactlessMifareProtocol()!!.readerProtocolName,
                getContactlessMifareProtocol()!!.applicationProtocolName
            )

            // with this protocol settings we activate the nfc for ISO1443_4 protocol
            (poReader as ObservableReader).activateProtocol(
                getContactlessIsoProtocol()!!.readerProtocolName,
                getContactlessIsoProtocol()!!.applicationProtocolName
            )
        }
        return poReader
    }

    @Throws(KeypleException::class)
    override suspend fun initSamReaders(): Map<String, Reader> {
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
            samReaders = SmartCardService.getInstance().getPlugin(PLUGIN_NAME).readers
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
            (it.value as AbstractLocalReader).activateProtocol(
                getSamReaderProtocol(),
                getSamReaderProtocol()
            )
        }

        return samReaders
    }

    override fun getSamReader(): Reader? {
        return if (samReaders.isNotEmpty()) {
            samReaders.values.first()
        } else {
            null
        }
    }

    override fun getContactlessIsoProtocol(): PoReaderProtocol? {
        return PoReaderProtocol(
            ContactlessCardCommonProtocols.ISO_14443_4.name,
            AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name)
        )
    }

    override fun getContactlessMifareProtocol(): PoReaderProtocol? {
        return PoReaderProtocol(
            AndroidNfcSupportedProtocols.MIFARE_CLASSIC.name,
            AndroidNfcProtocolSettings.getSetting(AndroidNfcSupportedProtocols.MIFARE_CLASSIC.name)
        )
    }

    override fun getSamReaderProtocol(): String {
        return ContactCardCommonProtocols.ISO_7816_3.name
    }

    override fun clear() {

        samReaders.forEach {
            (it.value as AbstractLocalReader).deactivateProtocol(getSamReaderProtocol())
        }

        poReader?.let {
            (poReader as ObservableReader).deactivateProtocol(getContactlessMifareProtocol()!!.readerProtocolName)
            (poReader as ObservableReader).deactivateProtocol(getContactlessIsoProtocol()!!.readerProtocolName)
        }
    }

    companion object {
        private const val MAX_TRIES = 10
    }
}
