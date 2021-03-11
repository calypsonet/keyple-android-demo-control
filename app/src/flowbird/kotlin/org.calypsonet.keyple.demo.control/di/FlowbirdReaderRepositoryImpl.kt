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
 ************************dis********************************************************/
package org.calypsonet.keyple.demo.control.di

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.demo.control.reader.PoReaderProtocol
import org.eclipse.keyple.core.plugin.AbstractLocalReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardService
import org.eclipse.keyple.core.service.event.ReaderObservationExceptionHandler
import org.eclipse.keyple.core.service.exception.KeypleException
import org.eclipse.keyple.flowbird.plugin.FlowbirdContactReader
import org.eclipse.keyple.flowbird.plugin.FlowbirdContactlessReader
import org.eclipse.keyple.flowbird.plugin.FlowbirdPlugin
import org.eclipse.keyple.flowbird.plugin.FlowbirdPluginFactory
import org.eclipse.keyple.flowbird.plugin.FlowbirdSupportContactlessProtocols
import org.eclipse.keyple.flowbird.plugin.SamSlot
import javax.inject.Inject

class FlowbirdReaderRepositoryImpl @Inject constructor(
    private val readerObservationExceptionHandler: ReaderObservationExceptionHandler
) :
    IReaderRepository {

    override var poReader: Reader? = null
    override var samReaders: MutableMap<String, Reader> = mutableMapOf()

    @Throws(KeypleException::class)
    override fun registerPlugin(activity: Activity) {
        runBlocking {
            val pluginFactory: FlowbirdPluginFactory?
            pluginFactory = withContext(Dispatchers.IO) {
                val mediaFiles: List<String> =
                    listOf("1_default_en.xml", "success.mp3", "error.mp3")
                val situationFiles: List<String> = listOf("1_default_en.xml")
                val translationFiles: List<String> = listOf("0_default.xml")
                FlowbirdPluginFactory.init(
                    activity = activity,
                    readerObservationExceptionHandler = readerObservationExceptionHandler,
                    mediaFiles = mediaFiles,
                    situationFiles = situationFiles,
                    translationFiles = translationFiles
                )
            }
            SmartCardService.getInstance().registerPlugin(pluginFactory)
        }
    }

    @Throws(KeypleException::class)
    override suspend fun initPoReader(): Reader? {
        val plugin =
            SmartCardService.getInstance().getPlugin(FlowbirdPluginFactory.pluginName)
        val poReader = plugin?.getReader(FlowbirdContactlessReader.READER_NAME)
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
        val plugin =
            SmartCardService.getInstance().getPlugin(FlowbirdPluginFactory.pluginName)
        samReaders = plugin?.readers?.filter {
            !it.value.isContactless
        }?.toMutableMap() ?: mutableMapOf()

        if (!getSamReaderProtocol().isNullOrEmpty()) {
            samReaders.forEach {
                (it.value as AbstractLocalReader).activateProtocol(
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
            FlowbirdSupportContactlessProtocols.ALL.key,
            FlowbirdSupportContactlessProtocols.ALL.key
        )
    }

    override fun getSamReaderProtocol(): String? = null

    override fun clear() {
        poReader?.deactivateProtocol(getContactlessIsoProtocol().readerProtocolName)

        if (!getSamReaderProtocol().isNullOrEmpty()) {
            samReaders.forEach {
                (it.value as AbstractLocalReader).deactivateProtocol(
                    getSamReaderProtocol()
                )
            }
        }
    }

    override fun displayWaiting(): Boolean {
        val plugin = SmartCardService.getInstance().getPlugin(FlowbirdPluginFactory.pluginName)
        plugin?.let {
            (it as FlowbirdPlugin).displayWaiting()
            return true
        }
        return false
    }

    override fun displayResultSuccess(): Boolean {
        val plugin = SmartCardService.getInstance().getPlugin(FlowbirdPluginFactory.pluginName)
        plugin?.let {
            (it as FlowbirdPlugin).displayResultSuccess()
            return true
        }
        return false
    }

    override fun displayResultFailed(): Boolean {
        val plugin = SmartCardService.getInstance().getPlugin(FlowbirdPluginFactory.pluginName)
        plugin?.let {
            (it as FlowbirdPlugin).displayResultFailed()
            return true
        }
        return false
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    companion object{
        @Suppress("unused")
        val SAM_READER_1_NAME =
            "${FlowbirdContactReader.READER_NAME}_${(SamSlot.ONE.slotId)}"
        @Suppress("unused")
        val SAM_READER_2_NAME =
            "${FlowbirdContactReader.READER_NAME}_${(SamSlot.TWO.slotId)}"
        @Suppress("unused")
        val SAM_READER_3_NAME =
            "${FlowbirdContactReader.READER_NAME}_${(SamSlot.THREE.slotId)}"
        @Suppress("unused")
        val SAM_READER_4_NAME =
            "${FlowbirdContactReader.READER_NAME}_${(SamSlot.FOUR.slotId)}"
    }
}

