/*
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
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
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.eclipse.keyple.bluebird.plugin.BluebirdContactReader
import org.eclipse.keyple.bluebird.plugin.BluebirdContactlessReader
import org.eclipse.keyple.bluebird.plugin.BluebirdPlugin
import org.eclipse.keyple.bluebird.plugin.BluebirdPluginFactory
import org.eclipse.keyple.bluebird.plugin.BluebirdSupportContactlessProtocols
import org.eclipse.keyple.core.plugin.AbstractLocalReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardService
import org.eclipse.keyple.core.service.event.ReaderObservationExceptionHandler
import org.eclipse.keyple.core.service.exception.KeypleException
import org.eclipse.keyple.core.service.util.ContactCardCommonProtocols
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.reader.PoReaderProtocol
import org.calypsonet.keyple.demo.control.R
import javax.inject.Inject

class BluebirdReaderRepositoryImpl @Inject constructor(
    private val readerObservationExceptionHandler: ReaderObservationExceptionHandler
) :
    IReaderRepository {

    lateinit var successMedia: MediaPlayer
    lateinit var errorMedia: MediaPlayer

    override var poReader: Reader? = null
    override var samReaders: MutableMap<String, Reader> = mutableMapOf()

    @Throws(KeypleException::class)
    override fun registerPlugin(activity: Activity) {
        runBlocking {

            successMedia = MediaPlayer.create(activity, R.raw.success)
            errorMedia = MediaPlayer.create(activity, R.raw.error)

            val pluginFactory: BluebirdPluginFactory?
            pluginFactory = withContext(Dispatchers.IO) {
                BluebirdPluginFactory.init(activity, readerObservationExceptionHandler)
            }
            SmartCardService.getInstance().registerPlugin(pluginFactory)
        }
    }

    @Throws(KeypleException::class)
    override suspend fun initPoReader(): Reader? {
        val bluebirdPlugin =
            SmartCardService.getInstance().getPlugin(BluebirdPluginFactory.pluginName)
        val poReader = bluebirdPlugin?.getReader(BluebirdContactlessReader.READER_NAME)
        poReader?.let {

            it.activateProtocol(
                getContactlessIsoProtocol().readerProtocolName,
                getContactlessIsoProtocol().applicationProtocolName
            )

            this.poReader = poReader
        }

        return poReader
    }

    @Throws(KeypleException::class)
    override suspend fun initSamReaders(): Map<String, Reader> {
        val askPlugin =
            SmartCardService.getInstance().getPlugin(BluebirdPluginFactory.pluginName)
        samReaders = askPlugin?.readers?.filter {
            !it.value.isContactless
        }?.toMutableMap() ?: mutableMapOf()

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
            val filteredByName = samReaders.filter {
                it.value.name == BluebirdContactReader.READER_NAME
            }

            return if (filteredByName.isNullOrEmpty()) {
                samReaders.values.first()
            } else {
                filteredByName.values.first()
            }
        } else {
            null
        }
    }

    override fun getContactlessIsoProtocol(): PoReaderProtocol {
        return PoReaderProtocol(
            BluebirdSupportContactlessProtocols.NFC_B_BB.key,
            BluebirdSupportContactlessProtocols.NFC_B_BB.key
        )
    }

    override fun getSamReaderProtocol(): String =
        ContactCardCommonProtocols.ISO_7816_3.name

    override fun clear() {
        poReader?.deactivateProtocol(getContactlessIsoProtocol().readerProtocolName)

        samReaders.forEach {
            (it.value as AbstractLocalReader).deactivateProtocol(
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
}

