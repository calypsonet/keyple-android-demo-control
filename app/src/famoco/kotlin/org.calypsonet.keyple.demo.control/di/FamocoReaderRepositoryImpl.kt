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
import org.eclipse.keyple.core.plugin.AbstractLocalReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardService
import org.eclipse.keyple.core.service.event.ObservableReader
import org.eclipse.keyple.core.service.event.ReaderObservationExceptionHandler
import org.eclipse.keyple.core.service.exception.KeypleException
import org.eclipse.keyple.core.service.util.ContactCardCommonProtocols
import org.eclipse.keyple.core.service.util.ContactlessCardCommonProtocols
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.reader.PoReaderProtocol
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPlugin
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPluginFactory
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactory
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcProtocolSettings
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import timber.log.Timber
import javax.inject.Inject

class FamocoReaderRepositoryImpl @Inject constructor(private val readerObservationExceptionHandler: ReaderObservationExceptionHandler) :
    IReaderRepository {

    override var poReader: Reader? = null
    override var samReaders: MutableMap<String, Reader> = mutableMapOf()

    @Throws(KeypleException::class)
    override fun registerPlugin(activity: Activity) {
        SmartCardService.getInstance().registerPlugin(AndroidNfcPluginFactory(activity, readerObservationExceptionHandler))
        try {
            SmartCardService.getInstance().registerPlugin(AndroidFamocoPluginFactory())
        } catch (e: UnsatisfiedLinkError) {
            Timber.w(e)
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

            // with this protocol settings we activate the nfc for ISO1443_4 protocol
            (poReader as ObservableReader).activateProtocol(
                getContactlessIsoProtocol()!!.readerProtocolName,
                getContactlessIsoProtocol()!!.applicationProtocolName
            )

            (poReader as ObservableReader).activateProtocol(
                getContactlessMifareProtocol()!!.readerProtocolName,
                getContactlessMifareProtocol()!!.applicationProtocolName
            )
        }

        return poReader
    }

    @Throws(KeypleException::class)
    override suspend fun initSamReaders(): Map<String, Reader> {
        if (samReaders.isNullOrEmpty()) {
            val samPlugin = SmartCardService.getInstance().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME)

            if (samPlugin != null) {
                val samReader = samPlugin.getReader(AndroidFamocoReader.READER_NAME)
                samReader?.let {
                    (it as AbstractLocalReader).activateProtocol(
                        getSamReaderProtocol(),
                        getSamReaderProtocol()
                    )

                    samReaders[AndroidFamocoReader.READER_NAME] = it
                }
            }
        }

        return samReaders
    }

    override fun getSamReader(): Reader? {
        return samReaders[AndroidFamocoReader.READER_NAME]
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
        poReader?.let {
            // with this protocol settings we activate the nfc for ISO1443_4 protocol
            it.deactivateProtocol(getContactlessIsoProtocol()!!.readerProtocolName)
            it.deactivateProtocol(getContactlessMifareProtocol()!!.readerProtocolName)
        }

        val samReader = samReaders[AndroidFamocoReader.READER_NAME]
        samReader?.deactivateProtocol(getSamReaderProtocol())
    }
}
