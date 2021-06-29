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
import org.eclipse.keyple.coppernic.ask.plugin.Cone2ContactReader
import org.eclipse.keyple.coppernic.ask.plugin.Cone2ContactlessReader
import org.eclipse.keyple.coppernic.ask.plugin.Cone2PluginFactory
import org.eclipse.keyple.coppernic.ask.plugin.ParagonSupportedContactProtocols
import org.eclipse.keyple.coppernic.ask.plugin.ParagonSupportedContactlessProtocols
import org.eclipse.keyple.core.plugin.AbstractLocalReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardService
import org.eclipse.keyple.core.service.event.ReaderObservationExceptionHandler
import org.eclipse.keyple.core.service.exception.KeypleException
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.reader.PoReaderProtocol
import javax.inject.Inject

class CoppernicReaderRepositoryImpl @Inject constructor(private val applicationContext: Context, private val readerObservationExceptionHandler: ReaderObservationExceptionHandler) :
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

            val pluginFactory: Cone2PluginFactory?
            pluginFactory = withContext(Dispatchers.IO) {
                Cone2PluginFactory.init(applicationContext, readerObservationExceptionHandler)
            }
            SmartCardService.getInstance().registerPlugin(pluginFactory)
        }
    }

    @Throws(KeypleException::class)
    override suspend fun initPoReader(): Reader? {
        val askPlugin =
            SmartCardService.getInstance().getPlugin(Cone2PluginFactory.pluginName)
        val poReader = askPlugin?.getReader(Cone2ContactlessReader.READER_NAME)
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
            SmartCardService.getInstance().getPlugin(Cone2PluginFactory.pluginName)
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
                it.value.name == SAM_READER_1_NAME
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
            ParagonSupportedContactlessProtocols.ISO_14443.name,
            ParagonSupportedContactlessProtocols.ISO_14443.name
        )
    }

    override fun getSamReaderProtocol(): String = ParagonSupportedContactProtocols.INNOVATRON_HIGH_SPEED_PROTOCOL.name

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
    }
}
