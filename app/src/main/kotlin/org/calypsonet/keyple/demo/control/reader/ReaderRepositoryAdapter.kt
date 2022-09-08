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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactReader
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactlessReader
import org.calypsonet.keyple.plugin.bluebird.BluebirdPlugin
import org.calypsonet.keyple.plugin.bluebird.BluebirdPluginFactoryProvider
import org.calypsonet.keyple.plugin.bluebird.BluebirdSupportContactlessProtocols
import org.calypsonet.keyple.plugin.coppernic.*
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoPlugin
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoPluginFactoryProvider
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoReader
import org.calypsonet.keyple.plugin.famoco.utils.ContactCardCommonProtocols
import org.calypsonet.keyple.plugin.flowbird.FlowbirdPlugin
import org.calypsonet.keyple.plugin.flowbird.FlowbirdPluginFactoryProvider
import org.calypsonet.keyple.plugin.flowbird.FlowbirdUiManager
import org.calypsonet.keyple.plugin.flowbird.contact.FlowbirdContactReader
import org.calypsonet.keyple.plugin.flowbird.contact.SamSlot
import org.calypsonet.keyple.plugin.flowbird.contactless.FlowbirdContactlessReader
import org.calypsonet.keyple.plugin.flowbird.contactless.FlowbirdSupportContactlessProtocols
import org.calypsonet.terminal.reader.CardReader
import org.calypsonet.terminal.reader.ConfigurableCardReader
import org.calypsonet.terminal.reader.ObservableCardReader
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPlugin
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPluginFactoryProvider
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiReader
import timber.log.Timber

class ReaderRepositoryAdapter
@Inject
constructor(
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) : ReaderRepository {

  private lateinit var readerType: ReaderType
  // Card
  private lateinit var cardPluginName: String
  private lateinit var cardReaderName: String
  private lateinit var cardReaderProtocolPhysicalName: String
  private lateinit var cardReaderProtocolLogicalName: String
  private var cardReader: CardReader? = null
  // SAM
  private lateinit var samPluginName: String
  private lateinit var samReaderNameRegex: String
  private lateinit var samReaderName: String
  private var samReaderProtocolPhysicalName: String? = null
  private var samReaderProtocolLogicalName: String? = null
  private var samPermissions: Array<String>? = null
  private var samReaders: MutableList<CardReader> = mutableListOf()
  // IHM
  private lateinit var successMedia: MediaPlayer
  private lateinit var errorMedia: MediaPlayer

  private fun initReaderType() {
    // TODO Init reader type
    initBluebirdReader()
  }

  private fun initBluebirdReader() {
    readerType = ReaderType.BLUEBIRD
    cardPluginName = BluebirdPlugin.PLUGIN_NAME
    cardReaderName = BluebirdContactlessReader.READER_NAME
    cardReaderProtocolPhysicalName = BluebirdSupportContactlessProtocols.ISO_14443_4_B.name
    cardReaderProtocolLogicalName = BluebirdSupportContactlessProtocols.ISO_14443_4_B.name
    samPluginName = BluebirdPlugin.PLUGIN_NAME
    samReaderNameRegex = ".*ContactReader"
    samReaderName = BluebirdContactReader.READER_NAME
    samReaderProtocolPhysicalName = "ISO_7816_3"
    samReaderProtocolLogicalName = "ISO_7816_3"
    samPermissions = arrayOf(BluebirdPlugin.BLUEBIRD_SAM_PERMISSION)
  }

  private fun initCoppernicReader() {
    readerType = ReaderType.COPPERNIC
    cardPluginName = Cone2Plugin.PLUGIN_NAME
    cardReaderName = Cone2ContactlessReader.READER_NAME
    cardReaderProtocolPhysicalName = ParagonSupportedContactlessProtocols.ISO_14443.name
    cardReaderProtocolLogicalName = ParagonSupportedContactlessProtocols.ISO_14443.name
    samPluginName = Cone2Plugin.PLUGIN_NAME
    samReaderNameRegex = ".*ContactReader_1"
    samReaderName = "${Cone2ContactReader.READER_NAME}_1"
    samReaderProtocolPhysicalName =
        ParagonSupportedContactProtocols.INNOVATRON_HIGH_SPEED_PROTOCOL.name
    samReaderProtocolLogicalName =
        ParagonSupportedContactProtocols.INNOVATRON_HIGH_SPEED_PROTOCOL.name
    samPermissions = null
  }

  private fun initFamocoReader() {
    readerType = ReaderType.FAMOCO
    cardPluginName = AndroidNfcPlugin.PLUGIN_NAME
    cardReaderName = AndroidNfcReader.READER_NAME
    cardReaderProtocolPhysicalName = "ISO_14443_4"
    cardReaderProtocolLogicalName = "ISO_14443_4"
    samPluginName = AndroidFamocoPlugin.PLUGIN_NAME
    samReaderNameRegex = ".*FamocoReader"
    samReaderName = AndroidFamocoReader.READER_NAME
    samReaderProtocolPhysicalName = ContactCardCommonProtocols.ISO_7816_3.name
    samReaderProtocolLogicalName = ContactCardCommonProtocols.ISO_7816_3.name
    samPermissions = null
  }

  private fun initFlowbirdReader() {
    readerType = ReaderType.FLOWBIRD
    cardPluginName = FlowbirdPlugin.PLUGIN_NAME
    cardReaderName = FlowbirdContactlessReader.READER_NAME
    cardReaderProtocolPhysicalName = FlowbirdSupportContactlessProtocols.ALL.key
    cardReaderProtocolLogicalName = FlowbirdSupportContactlessProtocols.ALL.key
    samPluginName = FlowbirdPlugin.PLUGIN_NAME
    samReaderNameRegex = ".*ContactReader_0"
    samReaderName = "${FlowbirdContactReader.READER_NAME}_${(SamSlot.ONE.slotId)}"
    samReaderProtocolPhysicalName = null
    samReaderProtocolLogicalName = null
    samPermissions = null
  }

  private fun initOmapiReader() {
    readerType = ReaderType.OMAPI
    cardPluginName = AndroidNfcPlugin.PLUGIN_NAME
    cardReaderName = AndroidNfcReader.READER_NAME
    cardReaderProtocolPhysicalName = "ISO_14443_4"
    cardReaderProtocolLogicalName = "ISO_14443_4"
    samPluginName = AndroidOmapiPlugin.PLUGIN_NAME
    samReaderNameRegex = ""
    samReaderName = AndroidOmapiReader.READER_NAME_SIM_1
    samReaderProtocolPhysicalName = null
    samReaderProtocolLogicalName = null
    samPermissions = null
  }

  @Throws(KeyplePluginException::class)
  override fun registerPlugin(activity: Activity) {
    initReaderType()
    if (readerType != ReaderType.FLOWBIRD) {
      successMedia = MediaPlayer.create(activity, R.raw.success)
      errorMedia = MediaPlayer.create(activity, R.raw.error)
    }
    runBlocking {
      // Plugin
      val pluginFactory =
          withContext(Dispatchers.IO) {
            when (readerType) {
              ReaderType.BLUEBIRD -> BluebirdPluginFactoryProvider.getFactory(activity)
              ReaderType.COPPERNIC -> Cone2PluginFactoryProvider.getFactory(activity)
              ReaderType.FAMOCO -> AndroidNfcPluginFactoryProvider(activity).getFactory()
              ReaderType.FLOWBIRD -> { // Init files used to sounds and colors from assets
                val mediaFiles: List<String> =
                    listOf("1_default_en.xml", "success.mp3", "error.mp3")
                val situationFiles: List<String> = listOf("1_default_en.xml")
                val translationFiles: List<String> = listOf("0_default.xml")
                FlowbirdPluginFactoryProvider.getFactory(
                    activity = activity,
                    mediaFiles = mediaFiles,
                    situationFiles = situationFiles,
                    translationFiles = translationFiles)
              }
              ReaderType.OMAPI -> AndroidNfcPluginFactoryProvider(activity).getFactory()
            }
          }
      SmartCardServiceProvider.getService().registerPlugin(pluginFactory)
      // SAM plugin (if different of card plugin)
      if (readerType == ReaderType.FAMOCO) {
        val samPluginFactory =
            withContext(Dispatchers.IO) { AndroidFamocoPluginFactoryProvider.getFactory() }
        SmartCardServiceProvider.getService().registerPlugin(samPluginFactory)
      }
      if (readerType == ReaderType.OMAPI) {
        withContext(Dispatchers.IO) {
          AndroidOmapiPluginFactoryProvider(activity) {
            SmartCardServiceProvider.getService().registerPlugin(it)
          }
        }
      }
    }
  }

  @Throws(KeyplePluginException::class)
  override suspend fun initCardReader(): CardReader? {
    cardReader =
        SmartCardServiceProvider.getService().getPlugin(cardPluginName)?.getReader(cardReaderName)
    cardReader?.let {
      (it as ConfigurableCardReader).activateProtocol(
          cardReaderProtocolPhysicalName, cardReaderProtocolLogicalName)
      (cardReader as ObservableCardReader).setReaderObservationExceptionHandler(
          readerObservationExceptionHandler)
    }
    return cardReader
  }

  override fun getCardReader(): CardReader? {
    return cardReader
  }

  override fun getCardReaderProtocol(): CardReaderProtocol {
    return CardReaderProtocol(cardReaderProtocolPhysicalName, cardReaderProtocolLogicalName)
  }

  @Throws(KeyplePluginException::class)
  override suspend fun initSamReaders(): List<CardReader> {
    if (readerType == ReaderType.FAMOCO) {
      samReaders =
          SmartCardServiceProvider.getService()
              .getPlugin(samPluginName)
              ?.readers
              ?.filter { it.name == samReaderName }
              ?.toMutableList()
              ?: mutableListOf()
    } else if (readerType == ReaderType.OMAPI) {
      /*
       * Wait until OMAPI sam readers are available.
       * If we do not wait, no retries are made after calling 'SmartCardService.getInstance().getPlugin(PLUGIN_NAME).readers'
       * -> then no reader is returned
       */
      @Suppress("BlockingMethodInNonBlockingContext")
      runBlocking { delay(250) }
      for (x in 1..MAX_TRIES) {
        samReaders =
            SmartCardServiceProvider.getService().getPlugin(samPluginName)?.readers?.toMutableList()
                ?: mutableListOf()
        if (samReaders.isEmpty()) {
          Timber.d("No readers found in OMAPI Keyple Plugin")
          Timber.d("Retrying in 1 second")
          delay(1000)
        } else {
          Timber.d("Readers Found")
          break
        }
      }
    } else {
      samReaders =
          SmartCardServiceProvider.getService()
              .getPlugin(samPluginName)
              ?.readers
              ?.filter { !it.isContactless }
              ?.toMutableList()
              ?: mutableListOf()
    }
    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.activateProtocol(samReaderProtocolPhysicalName, samReaderProtocolLogicalName)
      }
    }
    return samReaders
  }

  override fun getSamPlugin(): Plugin =
      SmartCardServiceProvider.getService().getPlugin(samPluginName)

  override fun getSamReader(): CardReader? {
    return if (samReaders.isNotEmpty()) {
      val filteredByName = samReaders.filter { it.name == samReaderName }
      return if (filteredByName.isEmpty()) {
        samReaders.first()
      } else {
        filteredByName.first()
      }
    } else {
      null
    }
  }

  override fun getSamReaderNameRegex(): String = samReaderNameRegex

  override fun getSamReaderConfiguratorSpi(): ReaderConfiguratorSpi = ReaderConfigurator()

  override fun getSamPermissions(): Array<String>? = samPermissions

  override fun clear() {
    (cardReader as ConfigurableCardReader).deactivateProtocol(cardReaderProtocolPhysicalName)
    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.deactivateProtocol(samReaderProtocolPhysicalName)
      }
    }
    if (readerType != ReaderType.FLOWBIRD) {
      successMedia.stop()
      successMedia.release()
      errorMedia.stop()
      errorMedia.release()
    }
  }

  override fun displayWaiting(): Boolean {
    return if (readerType == ReaderType.FLOWBIRD) {
      FlowbirdUiManager.displayWaiting()
      true
    } else {
      false
    }
  }

  override fun displayResultSuccess(): Boolean {
    if (readerType == ReaderType.FLOWBIRD) {
      FlowbirdUiManager.displayResultSuccess()
    } else {
      successMedia.start()
    }
    return true
  }

  override fun displayResultFailed(): Boolean {
    if (readerType == ReaderType.FLOWBIRD) {
      FlowbirdUiManager.displayResultFailed()
    } else {
      errorMedia.start()
    }
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

  companion object {
    private const val MAX_TRIES = 10
  }
}
