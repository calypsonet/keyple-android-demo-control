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
package org.eclipse.keyple.demo.control.di

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.eclipse.keyple.bluebird.plugin.BluebirdContactReader
import org.eclipse.keyple.bluebird.plugin.BluebirdContactlessReader
import org.eclipse.keyple.bluebird.plugin.BluebirdPlugin
import org.eclipse.keyple.bluebird.plugin.BluebirdPluginFactoryProvider
import org.eclipse.keyple.bluebird.plugin.BluebirdSupportContactlessProtocols
import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.service.spi.ReaderObservationExceptionHandlerSpi
import org.eclipse.keyple.core.util.protocol.ContactCardCommonProtocol
import org.eclipse.keyple.demo.control.reader.IReaderRepository
import org.eclipse.keyple.demo.control.reader.PoReaderProtocol
import javax.inject.Inject

class BluebirdReaderRepositoryImpl @Inject constructor(
    private val readerObservationExceptionHandler: ReaderObservationExceptionHandlerSpi
) :
    IReaderRepository {

    override var poReader: Reader? = null
    override var samReaders: MutableMap<String, Reader> = mutableMapOf()

    @Throws(KeyplePluginException::class)
    override fun registerPlugin(activity: Activity) {
        runBlocking {
            val pluginFactory: KeyplePluginExtensionFactory?
            pluginFactory = withContext(Dispatchers.IO) {
                BluebirdPluginFactoryProvider.getFactory(activity)
            }
            val smartCardService = SmartCardServiceProvider.getService()
            smartCardService.registerPlugin(pluginFactory)
        }
    }

    @Throws(KeyplePluginException::class)
    override suspend fun initPoReader(): Reader? {
        val bluebirdPlugin =
            SmartCardServiceProvider.getService().getPlugin(BluebirdPlugin.PLUGIN_NAME)
        val poReader = bluebirdPlugin?.getReader(BluebirdContactlessReader.READER_NAME)
        poReader?.let {

            it.activateProtocol(
                getContactlessIsoProtocol()!!.readerProtocolName,
                getContactlessIsoProtocol()!!.applicationProtocolName
            )

            this.poReader = poReader
        }

        (poReader as ObservableReader).setReaderObservationExceptionHandler(
            readerObservationExceptionHandler
        )

        return poReader
    }

    @Throws(KeyplePluginException::class)
    override suspend fun initSamReaders(): Map<String, Reader> {
        val bluebirdPlugin =
            SmartCardServiceProvider.getService().getPlugin(BluebirdPlugin.PLUGIN_NAME)
        samReaders = bluebirdPlugin?.readers?.filter {
            !it.value.isContactless
        }?.toMutableMap() ?: mutableMapOf()

        samReaders.forEach { reader ->
            reader.value.activateProtocol(
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

    override fun getContactlessIsoProtocol(): PoReaderProtocol? {
        return PoReaderProtocol(
            BluebirdSupportContactlessProtocols.NFC_B_BB.key,
            BluebirdSupportContactlessProtocols.NFC_B_BB.key
        )
    }

    override fun getContactlessMifareProtocol(): PoReaderProtocol? {
        return null
    }

    override fun getSamReaderProtocol(): String =
        ContactCardCommonProtocol.ISO_7816_3.name

    override fun clear() {
        poReader?.deactivateProtocol(getContactlessIsoProtocol()!!.readerProtocolName)

        samReaders.forEach {
            it.value.deactivateProtocol(
                getSamReaderProtocol()
            )
        }
    }

    override fun getPermissions(): Array<String> = arrayOf(BluebirdPlugin.BLUEBIRD_SAM_PERMISSION)
}

