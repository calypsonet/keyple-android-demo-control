/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.demo.control.reader

import android.app.Activity
import android.media.MediaPlayer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactReader
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactlessReader
import org.calypsonet.keyple.plugin.bluebird.BluebirdPlugin
import org.calypsonet.keyple.plugin.bluebird.BluebirdPluginFactoryProvider
import org.calypsonet.keyple.plugin.bluebird.BluebirdSupportContactlessProtocols
import org.calypsonet.terminal.reader.CardReader
import org.calypsonet.terminal.reader.ConfigurableCardReader
import org.calypsonet.terminal.reader.ObservableCardReader
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi

class ReaderRepositoryImpl
@Inject
constructor(
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) : IReaderRepository {

  private lateinit var readerType: ReaderType
  private lateinit var pluginName: String
  // Card
  private lateinit var contactlessReaderName: String
  private lateinit var contactlessProtocolPhysicalName: String
  private lateinit var contactlessProtocolLogicalName: String
  // SAM
  private lateinit var contactReaderNameRegex: String
  private lateinit var contactReaderName: String
  private lateinit var contactProtocolPhysicalName: String
  private lateinit var contactProtocolLogicalName: String
  // Permissions
  private lateinit var permissions: Array<String>

  private lateinit var successMedia: MediaPlayer
  private lateinit var errorMedia: MediaPlayer

  override var cardReader: CardReader? = null
  override var samReaders: MutableList<CardReader> = mutableListOf()

  private fun initReaderType() {
    // TODO Init reader type
    readerType = ReaderType.BLUEBIRD
    pluginName = BluebirdPlugin.PLUGIN_NAME
    contactlessReaderName = BluebirdContactlessReader.READER_NAME
    contactlessProtocolPhysicalName = BluebirdSupportContactlessProtocols.NFC_ALL.key
    contactlessProtocolLogicalName = BluebirdSupportContactlessProtocols.NFC_ALL.key
    contactReaderNameRegex = ".*ContactReader"
    contactReaderName = BluebirdContactReader.READER_NAME
    contactProtocolPhysicalName = "ISO_7816_3"
    contactProtocolLogicalName = "ISO_7816_3"
    permissions = arrayOf(BluebirdPlugin.BLUEBIRD_SAM_PERMISSION)
  }

  @Throws(KeyplePluginException::class)
  override fun registerPlugin(activity: Activity) {
    initReaderType()
    successMedia = MediaPlayer.create(activity, R.raw.success)
    errorMedia = MediaPlayer.create(activity, R.raw.error)
    runBlocking {
      val pluginFactory =
          withContext(Dispatchers.IO) {
            when (readerType) {
              ReaderType.BLUEBIRD -> BluebirdPluginFactoryProvider.getFactory(activity)
              ReaderType.COPPERNIC -> null // TODO
              ReaderType.FAMOCO -> null // TODO
              ReaderType.FLOWBIRD -> null // TODO
              ReaderType.MOCK_SAM -> null // TODO
              ReaderType.OMAPI -> null // TODO
            }
          }
      SmartCardServiceProvider.getService().registerPlugin(pluginFactory)
    }
  }

  override fun getPlugin(): Plugin = SmartCardServiceProvider.getService().getPlugin(pluginName)

  @Throws(KeyplePluginException::class)
  override suspend fun initCardReader(): CardReader? {
    cardReader =
        SmartCardServiceProvider.getService()
            .getPlugin(pluginName)
            ?.getReader(contactlessReaderName)
    cardReader?.let {
      (it as ConfigurableCardReader).activateProtocol(
          contactlessProtocolPhysicalName, contactlessProtocolLogicalName)
      (cardReader as ObservableCardReader).setReaderObservationExceptionHandler(
          readerObservationExceptionHandler)
    }
    return cardReader
  }

  @Throws(KeyplePluginException::class)
  override suspend fun initSamReaders(): List<CardReader> {
    samReaders =
        SmartCardServiceProvider.getService()
            .getPlugin(pluginName)
            ?.readers
            ?.filter { !it.isContactless }
            ?.toMutableList()
            ?: mutableListOf()
    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.activateProtocol(contactProtocolPhysicalName, contactProtocolLogicalName)
      }
    }
    return samReaders
  }

  override fun getSamReader(): CardReader? {
    return if (samReaders.isNotEmpty()) {
      val filteredByName = samReaders.filter { it.name == contactReaderName }
      return if (filteredByName.isEmpty()) {
        samReaders.first()
      } else {
        filteredByName.first()
      }
    } else {
      null
    }
  }

  override fun getContactlessIsoProtocol(): CardReaderProtocol {
    return CardReaderProtocol(contactlessProtocolPhysicalName, contactlessProtocolLogicalName)
  }

  override fun getSamReaderProtocol(): String = contactProtocolPhysicalName

  override fun getSamRegex(): String = contactReaderNameRegex

  override fun getReaderConfiguratorSpi(): ReaderConfiguratorSpi = ReaderConfigurator()

  override fun clear() {
    (cardReader as ConfigurableCardReader).deactivateProtocol(contactlessProtocolPhysicalName)

    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.deactivateProtocol(contactProtocolPhysicalName)
      }
    }

    successMedia.stop()
    successMedia.release()

    errorMedia.stop()
    errorMedia.release()
  }

  override fun getPermissions(): Array<String> = permissions

  override fun displayResultSuccess(): Boolean {
    successMedia.start()
    return true
  }

  override fun displayResultFailed(): Boolean {
    errorMedia.start()
    return true
  }

  /**
   * CardReader configurator used by the card resource service to setup the SAM reader with the
   * required settings.
   */
  internal class ReaderConfigurator : ReaderConfiguratorSpi {
    /** {@inheritDoc} */
    override fun setupReader(reader: CardReader) {
      // NOP
    }
  }
}
