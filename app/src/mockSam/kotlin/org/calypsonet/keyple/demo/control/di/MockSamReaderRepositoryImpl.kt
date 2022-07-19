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

class MockSamReaderRepositoryImpl
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
      val pluginFactory =
          withContext(Dispatchers.IO) { AndroidNfcPluginFactoryProvider(activity).getFactory() }
      SmartCardServiceProvider.getService().registerPlugin(pluginFactory)
    }
  }

  override fun getPlugin(): Plugin =
      SmartCardServiceProvider.getService().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)

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
    samReaders = mutableListOf()
    return samReaders
  }

  override fun getSamReader(): CardReader? {
    return null
  }

  override fun getContactlessIsoProtocol(): CardReaderProtocol {
    return CardReaderProtocol("ISO_14443_4", "ISO_14443_4")
  }

  override fun getSamReaderProtocol(): String? = null

  override fun getSamRegex(): String? = null

  override fun getReaderConfiguratorSpi(): ReaderConfiguratorSpi? = null

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
}
