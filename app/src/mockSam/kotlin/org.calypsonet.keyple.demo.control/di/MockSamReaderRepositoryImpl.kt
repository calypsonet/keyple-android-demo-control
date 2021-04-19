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
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import timber.log.Timber
import javax.inject.Inject

/**
 *
 *  @author youssefamrani
 */

class MockSamReaderRepositoryImpl @Inject constructor(private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi) :
    IReaderRepository {

    lateinit var successMedia: MediaPlayer
    lateinit var errorMedia: MediaPlayer

    override var poReader: Reader? = null
    override var samReaders: MutableList<Reader> = mutableListOf()

    @Throws(KeyplePluginException::class)
    override fun registerPlugin(activity: Activity) {

        successMedia = MediaPlayer.create(activity, R.raw.success)
        errorMedia = MediaPlayer.create(activity, R.raw.error)

        SmartCardServiceProvider.getService()
            .registerPlugin(AndroidNfcPluginFactoryProvider(activity).getFactory())
    }

    override fun getPlugin(): Plugin = SmartCardServiceProvider.getService().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)

    @Throws(KeyplePluginException::class)
    override suspend fun initPoReader(): Reader? {
        val readerPlugin = SmartCardServiceProvider.getService().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)
        poReader = readerPlugin.getReader(AndroidNfcReader.READER_NAME)

        poReader?.let {
            Timber.d("Initialize SEProxy with Android Plugin")

            // define task as an observer for ReaderEvents
            Timber.d("PO (NFC) reader name: ${it.name}")

//            androidNfcReader.presenceCheckDelay = 100
//            androidNfcReader.noPlateformSound = false
//            androidNfcReader.skipNdefCheck = false

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

    override suspend fun initSamReaders(): List<Reader> {
        samReaders = mutableListOf()
        return samReaders
    }

    override fun getSamReader(): Reader? {
        return null
    }

    override fun getContactlessIsoProtocol(): PoReaderProtocol {
        return PoReaderProtocol(
            ContactlessCardCommonProtocol.ISO_14443_4.name,
            ContactlessCardCommonProtocol.ISO_14443_4.name
        )
    }

    override fun getSamReaderProtocol(): String? = null

    override fun getSamRegex(): String? = null

    override fun getReaderConfiguratorSpi(): ReaderConfiguratorSpi? = null

    override fun clear() {
        // with this protocol settings we activate the nfc for ISO1443_4 protocol
        poReader?.deactivateProtocol(getContactlessIsoProtocol().readerProtocolName)

        successMedia.stop()
        successMedia.release()

        errorMedia.stop()
        errorMedia.release()
    }

    override fun isMockedResponse(): Boolean {
        return true
    }

    override fun displayResultSuccess(): Boolean {
        successMedia.start()
        return true
    }

    override fun displayResultFailed(): Boolean {
        errorMedia.start()
        return true
    }

//    @Suppress("INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING")
//    class AndroidMockReaderImpl : AbstractLocalReader(
//        "",
//        ""
//    ) {
//
//        override fun transmitApdu(apduIn: ByteArray?): ByteArray {
//            return apduIn ?: throw IllegalStateException("Mock no apdu in")
//        }
//
//        override fun getATR(): ByteArray? {
//            return null
//        }
//
//        override fun openPhysicalChannel() {
//        }
//
//        override fun isPhysicalChannelOpen(): Boolean {
//            return true
//        }
//
//        override fun isCardPresent(): Boolean {
//            return true
//        }
//
//        override fun checkCardPresence(): Boolean {
//            return true
//        }
//
//        override fun closePhysicalChannel() {
//        }
//
//        override fun isContactless(): Boolean {
//            return false
//        }
//
//        override fun isCurrentProtocol(readerProtocolName: String?): Boolean {
//            return true
//        }
//
//        override fun deactivateReaderProtocol(readerProtocolName: String?) {
//            // Do nothing
//        }
//
//        override fun activateReaderProtocol(readerProtocolName: String?) {
//            // Do nothing
//        }
//
//        companion object {
//            const val READER_NAME = "Mock_Sam"
//        }
//    }
}
