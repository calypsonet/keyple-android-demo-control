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
package org.calypsonet.keyple.demo.control.service

import android.app.Activity
import javax.inject.Inject
import org.calypsonet.keyple.demo.control.android.di.scope.AppScoped
import org.calypsonet.keyple.demo.control.service.reader.ReaderService
import org.calypsonet.keyple.demo.control.service.reader.ReaderType
import org.calypsonet.keyple.demo.control.service.ticketing.TicketingService
import org.calypsonet.terminal.reader.CardReader
import org.calypsonet.terminal.reader.ObservableCardReader
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import timber.log.Timber

@AppScoped
class MainService @Inject constructor(private var readerService: ReaderService) {

  private var ticketingService: TicketingService? = null
  var readersInitialized = false

  @Throws(KeyplePluginException::class, IllegalStateException::class, Exception::class)
  fun init(observer: CardReaderObserverSpi?, activity: Activity, readerType: ReaderType) {
    // Register plugin
    try {
      readerService.registerPlugin(activity, readerType)
    } catch (e: Exception) {
      Timber.e(e)
      throw IllegalStateException(e.message)
    }
    // Init card reader
    val cardReader: CardReader?
    try {
      cardReader = readerService.initCardReader()
    } catch (e: Exception) {
      Timber.e(e)
      throw IllegalStateException(e.message)
    }
    // Init SAM reader
    var samReaders: List<CardReader>? = null
    try {
      samReaders = readerService.initSamReaders()
    } catch (e: Exception) {
      Timber.e(e)
    }
    if (samReaders.isNullOrEmpty()) {
      Timber.w("No SAM reader available")
    }
    // Register a card event observer and init the ticketing session
    cardReader?.let { reader ->
      (reader as ObservableCardReader).addObserver(observer)
      ticketingService = TicketingService(readerService)
    }
  }

  fun startNfcDetection() {
    // Provide the CardReader with the selection operation to be processed when a Card is inserted.
    ticketingService?.prepareAndScheduleCardSelectionScenario()
    (readerService.getCardReader() as ObservableCardReader).startCardDetection(
        ObservableCardReader.DetectionMode.REPEATING)
  }

  fun stopNfcDetection() {
    try {
      // notify reader that se detection has been switched off
      (readerService.getCardReader() as ObservableCardReader).stopCardDetection()
    } catch (e: KeyplePluginException) {
      Timber.e(e, "NFC Plugin not found")
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  fun getTicketingSession(): TicketingService? {
    return ticketingService
  }

  fun onDestroy(observer: CardReaderObserverSpi?) {
    readersInitialized = false
    readerService.clear()
    if (observer != null && readerService.getCardReader() != null) {
      (readerService.getCardReader() as ObservableCardReader).removeObserver(observer)
    }
    val smartCardService = SmartCardServiceProvider.getService()
    smartCardService.plugins.forEach { smartCardService.unregisterPlugin(it.name) }
    ticketingService = null
  }

  fun displayResultSuccess(): Boolean = readerService.displayResultSuccess()

  fun displayResultFailed(): Boolean = readerService.displayResultFailed()
}
