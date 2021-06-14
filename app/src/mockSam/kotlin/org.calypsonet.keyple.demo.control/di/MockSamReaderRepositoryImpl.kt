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
import javax.inject.Inject
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
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactory
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcProtocolSettings
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import timber.log.Timber

/**
 *
 *  @author youssefamrani
 */

class MockSamReaderRepositoryImpl @Inject constructor(private val readerObservationExceptionHandler: ReaderObservationExceptionHandler) :
    IReaderRepository {

    override var poReader: Reader? = null
    override var samReaders: MutableMap<String, Reader> = mutableMapOf()

    @Throws(KeypleException::class)
    override fun registerPlugin(activity: Activity) {
        SmartCardService.getInstance().registerPlugin(AndroidNfcPluginFactory(activity, readerObservationExceptionHandler))
    }

    @Throws(KeypleException::class)
    override suspend fun initPoReader(): Reader? {
        val readerPlugin = SmartCardService.getInstance().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)
        poReader = readerPlugin.readers.values.first()

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
        samReaders =
            mutableMapOf(Pair(AndroidMockReaderImpl.READER_NAME, AndroidMockReaderImpl()))

        return samReaders
    }

    override fun getSamReader(): Reader? {
        return samReaders[AndroidMockReaderImpl.READER_NAME]
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
        // with this protocol settings we activate the nfc for ISO1443_4 protocol
        (poReader as ObservableReader).deactivateProtocol(getContactlessIsoProtocol()!!.readerProtocolName)
        (poReader as ObservableReader).deactivateProtocol(getContactlessMifareProtocol()!!.readerProtocolName)
    }

    override fun isMockedResponse(): Boolean {
        return true
    }

    @Suppress("INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING")
    class AndroidMockReaderImpl : AbstractLocalReader(
        "",
        ""
    ) {

        override fun transmitApdu(apduIn: ByteArray?): ByteArray {
            return apduIn ?: throw IllegalStateException("Mock no apdu in")
        }

        override fun getATR(): ByteArray? {
            return null
        }

        override fun openPhysicalChannel() {
        }

        override fun isPhysicalChannelOpen(): Boolean {
            return true
        }

        override fun isCardPresent(): Boolean {
            return true
        }

        override fun checkCardPresence(): Boolean {
            return true
        }

        override fun closePhysicalChannel() {
        }

        override fun isContactless(): Boolean {
            return false
        }

        override fun isCurrentProtocol(readerProtocolName: String?): Boolean {
            return true
        }

        override fun deactivateReaderProtocol(readerProtocolName: String?) {
            // Do nothing
        }

        override fun activateReaderProtocol(readerProtocolName: String?) {
            // Do nothing
        }

        companion object {
            const val READER_NAME = "Mock_Sam"
        }
    }
}
