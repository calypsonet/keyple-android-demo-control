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
package org.calypsonet.keyple.demo.control.di

import android.app.Activity
import android.media.MediaPlayer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.reader.CardReaderProtocol
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoPlugin
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoPluginFactoryProvider
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoReader
import org.calypsonet.keyple.plugin.famoco.utils.ContactCardCommonProtocols
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

class FamocoReaderRepositoryImpl
@Inject
constructor(
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) : IReaderRepository {

  private lateinit var successMedia: MediaPlayer
  private lateinit var errorMedia: MediaPlayer

  override var cardReader: CardReader? = null
  override var samReaders: MutableList<CardReader> = mutableListOf()

  @Throws(KeyplePluginException::class)
  override fun registerPlugin(activity: Activity) {
    successMedia = MediaPlayer.create(activity, R.raw.success)
    errorMedia = MediaPlayer.create(activity, R.raw.error)
    runBlocking {
      val nfcPluginFactory =
          withContext(Dispatchers.IO) { AndroidNfcPluginFactoryProvider(activity).getFactory() }
      SmartCardServiceProvider.getService().registerPlugin(nfcPluginFactory)

      val famocoPluginFactory =
          withContext(Dispatchers.IO) { AndroidFamocoPluginFactoryProvider.getFactory() }
      SmartCardServiceProvider.getService().registerPlugin(famocoPluginFactory)
    }
  }

  override fun getPlugin(): Plugin =
      SmartCardServiceProvider.getService().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME)

  @Throws(KeyplePluginException::class)
  override suspend fun initCardReader(): CardReader? {
    val plugin = SmartCardServiceProvider.getService().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)
    cardReader = plugin?.getReader(AndroidNfcReader.READER_NAME)
    cardReader?.let {
      (it as ConfigurableCardReader).activateProtocol(
          getContactlessIsoProtocol().readerProtocolName,
          getContactlessIsoProtocol().applicationProtocolName)
      (cardReader as ObservableCardReader).setReaderObservationExceptionHandler(
          readerObservationExceptionHandler)
    }
    return cardReader
  }

  @Throws(KeyplePluginException::class)
  override suspend fun initSamReaders(): List<CardReader> {
    val plugin = SmartCardServiceProvider.getService().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME)
    samReaders =
        plugin?.readers?.filter { it.name == AndroidFamocoReader.READER_NAME }?.toMutableList()
            ?: mutableListOf()
    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.activateProtocol(getSamReaderProtocol(), getSamReaderProtocol())
      }
    }
    return samReaders
  }

  override fun getSamReader(): CardReader? {
    return if (samReaders.isNotEmpty()) {
      val filteredByName = samReaders.filter { it.name == AndroidFamocoReader.READER_NAME }
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
    return CardReaderProtocol("ISO_14443_4", "ISO_14443_4")
  }

  override fun getSamReaderProtocol(): String = ContactCardCommonProtocols.ISO_7816_3.name

  override fun getSamRegex(): String = SAM_READER_NAME_REGEX

  override fun getReaderConfiguratorSpi(): ReaderConfiguratorSpi = ReaderConfigurator()

  override fun clear() {
    (cardReader as ConfigurableCardReader).deactivateProtocol(
        getContactlessIsoProtocol().readerProtocolName)

    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.deactivateProtocol(getSamReaderProtocol())
      }
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
    const val SAM_READER_NAME_REGEX = ".*FamocoReader"
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
