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
import kotlinx.coroutines.delay
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
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPluginFactoryProvider
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiReader
import timber.log.Timber

class OmapiReaderRepositoryImpl
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

      withContext(Dispatchers.IO) {
        AndroidOmapiPluginFactoryProvider(activity) {
          SmartCardServiceProvider.getService().registerPlugin(it)
        }
      }
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
    /*
     * Wait until OMAPI sam readers are available.
     * If we do not wait, no retries are made after calling 'SmartCardService.getInstance().getPlugin(PLUGIN_NAME).readers'
     * -> then no reader is returned
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    runBlocking { delay(250) }
    for (x in 1..MAX_TRIES) {
      val plugin = SmartCardServiceProvider.getService().getPlugin(AndroidNfcPlugin.PLUGIN_NAME)
      samReaders = plugin?.readers?.toMutableList() ?: mutableListOf()
      if (samReaders.isEmpty()) {
        Timber.d("No readers found in OMAPI Keyple Plugin")
        Timber.d("Retrying in 1 second")
        delay(1000)
      } else {
        Timber.d("Readers Found")
        break
      }
    }
    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.activateProtocol(getSamReaderProtocol(), getSamReaderProtocol())
      }
    }
    return samReaders
  }

  override fun getSamReader(): CardReader? {
    return if (samReaders.isNotEmpty()) {
      val filteredByName = samReaders.filter { it.name == AndroidOmapiReader.READER_NAME_SIM_1 }
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

  override fun getSamReaderProtocol(): String? = null

  override fun getSamRegex(): String = ""

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
    private const val MAX_TRIES = 10
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
