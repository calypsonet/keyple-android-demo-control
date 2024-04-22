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
package org.calypsonet.keyple.demo.control.domain

import android.app.Activity
import java.time.LocalDateTime
import javax.inject.Inject
import org.calypsonet.keyple.demo.common.constant.CardConstant
import org.calypsonet.keyple.demo.control.data.CardRepository
import org.calypsonet.keyple.demo.control.data.ReaderRepository
import org.calypsonet.keyple.demo.control.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.data.model.Location
import org.calypsonet.keyple.demo.control.data.model.ReaderType
import org.calypsonet.keyple.demo.control.di.scope.AppScoped
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.ReaderApiFactory
import org.eclipse.keypop.reader.selection.CardSelectionManager
import org.eclipse.keypop.reader.selection.CardSelectionResult
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

@AppScoped
class TicketingService @Inject constructor(private var readerRepository: ReaderRepository) {

  private val readerApiFactory: ReaderApiFactory =
      SmartCardServiceProvider.getService().readerApiFactory
  private val calypsoExtensionService: CalypsoExtensionService =
      CalypsoExtensionService.getInstance()
  private val calypsoCardApiFactory: CalypsoCardApiFactory =
      calypsoExtensionService.calypsoCardApiFactory
  private lateinit var legacySam: LegacySam
  private lateinit var calypsoCard: CalypsoCard
  private lateinit var cardSelectionManager: CardSelectionManager

  var readersInitialized = false
    private set

  var isSecureSessionMode: Boolean = false
    private set

  private var indexOfKeypleGenericCardSelection = 0
  private var indexOfCdLightGtmlCardSelection = 0
  private var indexOfCalypsoLightCardSelection = 0
  private var indexOfNavigoIdfCardSelection = 0

  @Throws(KeyplePluginException::class, IllegalStateException::class, Exception::class)
  fun init(observer: CardReaderObserverSpi?, activity: Activity, readerType: ReaderType) {
    // Register plugin
    try {
      readerRepository.registerPlugin(activity, readerType)
    } catch (e: Exception) {
      Timber.e(e)
      throw IllegalStateException(e.message)
    }
    // Init card reader
    val cardReader: CardReader?
    try {
      cardReader = readerRepository.initCardReader()
    } catch (e: Exception) {
      Timber.e(e)
      throw IllegalStateException(e.message)
    }
    // Init SAM reader
    var samReaders: List<CardReader>? = null
    try {
      samReaders = readerRepository.initSamReaders()
    } catch (e: Exception) {
      Timber.e(e)
    }
    if (samReaders.isNullOrEmpty()) {
      Timber.w("No SAM reader available")
    }
    // Register a card event observer and init the ticketing session
    cardReader?.let { reader ->
      (reader as ObservableCardReader).addObserver(observer)
      // attempts to select a SAM if any, sets the isSecureSessionMode flag accordingly
      val samReader = readerRepository.getSamReader()
      isSecureSessionMode = samReader != null && selectSam(samReader)
    }
    readersInitialized = true
  }

  fun startNfcDetection() {
    // Provide the CardReader with the selection operation to be processed when a Card is inserted.
    prepareAndScheduleCardSelectionScenario()
    (readerRepository.getCardReader() as ObservableCardReader).startCardDetection(
        ObservableCardReader.DetectionMode.REPEATING)
  }

  fun stopNfcDetection() {
    try {
      // notify reader that se detection has been switched off
      (readerRepository.getCardReader() as ObservableCardReader).stopCardDetection()
    } catch (e: KeyplePluginException) {
      Timber.e(e, "NFC Plugin not found")
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  fun onDestroy(observer: CardReaderObserverSpi?) {
    readersInitialized = false
    readerRepository.clear()
    if (observer != null && readerRepository.getCardReader() != null) {
      (readerRepository.getCardReader() as ObservableCardReader).removeObserver(observer)
    }
    val smartCardService = SmartCardServiceProvider.getService()
    smartCardService.plugins.forEach { smartCardService.unregisterPlugin(it.name) }
  }

  fun displayResultSuccess(): Boolean = readerRepository.displayResultSuccess()

  fun displayResultFailed(): Boolean = readerRepository.displayResultFailed()

  fun prepareAndScheduleCardSelectionScenario() {

    // Get the Keyple main service
    val smartCardService = SmartCardServiceProvider.getService()

    // Check the Calypso card extension
    smartCardService.checkCardExtension(calypsoExtensionService)

    // Get a new card selection manager
    cardSelectionManager = readerApiFactory.createCardSelectionManager()

    // Prepare card selection case #1: Keyple generic
    indexOfKeypleGenericCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstant.AID_KEYPLE_GENERIC)
                .filterByCardProtocol(readerRepository.getCardReaderProtocolLogicalName()),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #2: CD LIGHT/GTML
    indexOfCdLightGtmlCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstant.AID_CD_LIGHT_GTML)
                .filterByCardProtocol(readerRepository.getCardReaderProtocolLogicalName()),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #3: CALYPSO LIGHT
    indexOfCalypsoLightCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstant.AID_CALYPSO_LIGHT)
                .filterByCardProtocol(readerRepository.getCardReaderProtocolLogicalName()),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #4: Navigo IDF
    indexOfNavigoIdfCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstant.AID_NORMALIZED_IDF)
                .filterByCardProtocol(readerRepository.getCardReaderProtocolLogicalName()),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Schedule the execution of the prepared card selection scenario as soon as a card is presented
    cardSelectionManager.scheduleCardSelectionScenario(
        readerRepository.getCardReader() as ObservableCardReader,
        ObservableCardReader.NotificationMode.ALWAYS)
  }

  fun analyseSelectionResult(
      scheduledCardSelectionsResponse: ScheduledCardSelectionsResponse
  ): String? {
    Timber.i("selectionResponse = $scheduledCardSelectionsResponse")
    val cardSelectionResult: CardSelectionResult =
        cardSelectionManager.parseScheduledCardSelectionsResponse(scheduledCardSelectionsResponse)
    if (cardSelectionResult.activeSelectionIndex == -1) {
      return "Selection error: AID not found"
    }
    calypsoCard = cardSelectionResult.activeSmartCard as CalypsoCard
    // check is the DF name is the expected one (Req. TL-SEL-AIDMATCH.1)
    if ((cardSelectionResult.activeSelectionIndex == indexOfKeypleGenericCardSelection &&
        !CardConstant.aidMatch(CardConstant.AID_KEYPLE_GENERIC, calypsoCard.dfName)) ||
        (cardSelectionResult.activeSelectionIndex == indexOfCdLightGtmlCardSelection &&
            !CardConstant.aidMatch(CardConstant.AID_CD_LIGHT_GTML, calypsoCard.dfName)) ||
        (cardSelectionResult.activeSelectionIndex == indexOfCalypsoLightCardSelection &&
            !CardConstant.aidMatch(CardConstant.AID_CALYPSO_LIGHT, calypsoCard.dfName)) ||
        (cardSelectionResult.activeSelectionIndex == indexOfNavigoIdfCardSelection &&
            !CardConstant.aidMatch(CardConstant.AID_NORMALIZED_IDF, calypsoCard.dfName))) {
      return "Unexpected DF name"
    }
    if (calypsoCard.applicationSubtype !in CardConstant.ALLOWED_FILE_STRUCTURES) {
      return "Invalid card\nFile structure " +
          HexUtil.toHex(calypsoCard.applicationSubtype) +
          "h not supported"
    }
    Timber.i("Card DF Name = %s", HexUtil.toHex(calypsoCard.dfName))
    return null
  }

  fun executeControlProcedure(locations: List<Location>): CardReaderResponse {
    return CardRepository()
        .executeControlProcedure(
            cardReader = readerRepository.getCardReader()!!,
            calypsoCard = calypsoCard,
            cardSecuritySettings = if (isSecureSessionMode) getSecuritySettings() else null,
            locations = locations,
            controlDateTime = LocalDateTime.now())
  }

  private fun getSecuritySettings(): SymmetricCryptoSecuritySetting? {
    return calypsoCardApiFactory
        .createSymmetricCryptoSecuritySetting(
            LegacySamExtensionService.getInstance()
                .getLegacySamApiFactory()
                .createSymmetricCryptoCardTransactionManagerFactory(
                    readerRepository.getSamReader(), legacySam))
        .assignDefaultKif(
            WriteAccessLevel.PERSONALIZATION, CardConstant.DEFAULT_KIF_PERSONALIZATION)
        .assignDefaultKif(WriteAccessLevel.LOAD, CardConstant.DEFAULT_KIF_LOAD)
        .assignDefaultKif(WriteAccessLevel.DEBIT, CardConstant.DEFAULT_KIF_DEBIT)
        .enableMultipleSession()
  }

  private fun selectSam(samReader: CardReader): Boolean {

    // Create a SAM selection manager.
    val samSelectionManager: CardSelectionManager = readerApiFactory.createCardSelectionManager()

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(
        readerApiFactory
            .createBasicCardSelector()
            .filterByPowerOnData(
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null)),
        LegacySamExtensionService.getInstance()
            .legacySamApiFactory
            .createLegacySamSelectionExtension())
    try {
      // SAM communication: run the selection scenario.
      val samSelectionResult = samSelectionManager.processCardSelectionScenario(samReader)

      // Get the Calypso SAM SmartCard resulting of the selection.
      legacySam = samSelectionResult.activeSmartCard!! as LegacySam
      return true
    } catch (e: Exception) {
      Timber.e(e)
      Timber.e("An exception occurred while selecting the SAM.  ${e.message}")
    }
    return false
  }
}
