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
import java.util.*
import javax.inject.Inject
import org.calypsonet.keyple.demo.common.constant.CardConstant
import org.calypsonet.keyple.demo.control.data.CardRepository
import org.calypsonet.keyple.demo.control.data.ReaderRepository
import org.calypsonet.keyple.demo.control.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.data.model.Location
import org.calypsonet.keyple.demo.control.data.model.ReaderType
import org.calypsonet.keyple.demo.control.ui.di.scope.AppScoped
import org.calypsonet.terminal.calypso.WriteAccessLevel
import org.calypsonet.terminal.calypso.card.CalypsoCard
import org.calypsonet.terminal.calypso.sam.CalypsoSam
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting
import org.calypsonet.terminal.reader.CardReader
import org.calypsonet.terminal.reader.ObservableCardReader
import org.calypsonet.terminal.reader.selection.CardSelectionManager
import org.calypsonet.terminal.reader.selection.CardSelectionResult
import org.calypsonet.terminal.reader.selection.ScheduledCardSelectionsResponse
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.joda.time.DateTime
import timber.log.Timber

@AppScoped
class TicketingService @Inject constructor(private var readerRepository: ReaderRepository) {

  private val calypsoExtensionService: CalypsoExtensionService =
      CalypsoExtensionService.getInstance()

  private lateinit var calypsoSam: CalypsoSam
  private lateinit var calypsoCard: CalypsoCard
  private lateinit var cardSelectionManager: CardSelectionManager

  private val allowedFileStructures: EnumMap<FileStructureEnum, List<String>> =
      EnumMap(FileStructureEnum::class.java)

  private var indexOfCardSelectionAid1TicIca1 = 0
  private var indexOfCardSelectionAid1TicIca3 = 0
  private var indexOfCardSelectionAidIdf = 0

  private var fileStructure: FileStructureEnum? = null

  var cardAid: String? = null
    private set
  var readersInitialized = false
    private set
  var isSecureSessionMode: Boolean = false
    private set

  init {
    allowedFileStructures[FileStructureEnum.FILE_STRUCTURE_02H] =
        listOf(CardConstant.AID_1TIC_ICA_1)
    allowedFileStructures[FileStructureEnum.FILE_STRUCTURE_05H] =
        listOf(CardConstant.AID_1TIC_ICA_1, CardConstant.AID_NORMALIZED_IDF)
    allowedFileStructures[FileStructureEnum.FILE_STRUCTURE_32H] =
        listOf(CardConstant.AID_1TIC_ICA_3)
  }

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
    cardSelectionManager = smartCardService.createCardSelectionManager()

    // Prepare card selection case #1: 1 TIC ICA 1
    indexOfCardSelectionAid1TicIca1 =
        cardSelectionManager.prepareSelection(
            calypsoExtensionService
                .createCardSelection()
                .filterByDfName(CardConstant.AID_1TIC_ICA_1)
                .filterByCardProtocol(readerRepository.getCardReaderProtocolLogicalName()))

    // Prepare card selection case #2: 1 TIC ICA 3
    indexOfCardSelectionAid1TicIca3 =
        cardSelectionManager.prepareSelection(
            calypsoExtensionService
                .createCardSelection()
                .filterByDfName(CardConstant.AID_1TIC_ICA_3)
                .filterByCardProtocol(readerRepository.getCardReaderProtocolLogicalName()))

    // Prepare card selection case #3: Navigo
    indexOfCardSelectionAidIdf =
        cardSelectionManager.prepareSelection(
            calypsoExtensionService
                .createCardSelection()
                .filterByDfName(CardConstant.AID_NORMALIZED_IDF)
                .filterByCardProtocol(readerRepository.getCardReaderProtocolLogicalName()))

    // Schedule the execution of the prepared card selection scenario as soon as a card is presented
    cardSelectionManager.scheduleCardSelectionScenario(
        readerRepository.getCardReader() as ObservableCardReader,
        ObservableCardReader.DetectionMode.REPEATING,
        ObservableCardReader.NotificationMode.ALWAYS)
  }

  fun parseScheduledCardSelectionsResponse(
      selectionResponse: ScheduledCardSelectionsResponse?
  ): CardSelectionResult {
    Timber.i("selectionResponse = $selectionResponse")
    val cardSelectionResult: CardSelectionResult =
        cardSelectionManager.parseScheduledCardSelectionsResponse(selectionResponse)
    if (cardSelectionResult.activeSelectionIndex != -1) {
      when (cardSelectionResult.smartCards.keys.first()) {
        indexOfCardSelectionAid1TicIca1 -> {
          calypsoCard = cardSelectionResult.activeSmartCard as CalypsoCard
          fileStructure = FileStructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
          cardAid = CardConstant.AID_1TIC_ICA_1
        }
        indexOfCardSelectionAid1TicIca3 -> {
          calypsoCard = cardSelectionResult.activeSmartCard as CalypsoCard
          cardAid = CardConstant.AID_1TIC_ICA_3
          fileStructure = FileStructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
        }
        indexOfCardSelectionAidIdf -> {
          calypsoCard = cardSelectionResult.activeSmartCard as CalypsoCard
          cardAid = CardConstant.AID_NORMALIZED_IDF
          fileStructure = FileStructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
        }
        else -> cardAid = CardConstant.AID_OTHER
      }
    }
    Timber.i("Card AID = $cardAid")
    return cardSelectionResult
  }

  fun checkStructure(): Boolean {
    if (!allowedFileStructures.containsKey(fileStructure)) {
      return false
    }
    if (!allowedFileStructures[fileStructure]!!.contains(cardAid)) {
      return false
    }
    return true
  }

  fun executeControlProcedure(locations: List<Location>): CardReaderResponse {
    return CardRepository()
        .executeControlProcedure(
            cardReader = readerRepository.getCardReader()!!,
            calypsoCard = calypsoCard,
            cardSecuritySettings = if (isSecureSessionMode) getSecuritySettings() else null,
            locations = locations,
            now = DateTime.now())
  }

  private fun getSecuritySettings(): CardSecuritySetting? {
    return calypsoExtensionService
        .createCardSecuritySetting()
        .setControlSamResource(readerRepository.getSamReader(), calypsoSam)
        .assignDefaultKif(
            WriteAccessLevel.PERSONALIZATION, CardConstant.DEFAULT_KIF_PERSONALIZATION)
        .assignDefaultKif(WriteAccessLevel.LOAD, CardConstant.DEFAULT_KIF_LOAD)
        .assignDefaultKif(WriteAccessLevel.DEBIT, CardConstant.DEFAULT_KIF_DEBIT)
        .enableMultipleSession()
  }

  private fun selectSam(samReader: CardReader): Boolean {
    // Get the Keyple main service
    val smartCardService = SmartCardServiceProvider.getService()

    // Create a SAM selection manager.
    val samSelectionManager: CardSelectionManager = smartCardService.createCardSelectionManager()

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(
        calypsoExtensionService
            .createSamSelection()
            .filterByProductType(CalypsoSam.ProductType.SAM_C1))
    try {
      // SAM communication: run the selection scenario.
      val samSelectionResult = samSelectionManager.processCardSelectionScenario(samReader)

      // Get the Calypso SAM SmartCard resulting of the selection.
      calypsoSam = samSelectionResult.activeSmartCard!! as CalypsoSam
      return true
    } catch (e: Exception) {
      Timber.e(e)
      Timber.e("An exception occurred while selecting the SAM.  ${e.message}")
    }
    return false
  }

  private enum class FileStructureEnum(val key: Int) {
    FILE_STRUCTURE_02H(0x2),
    FILE_STRUCTURE_05H(0x5),
    FILE_STRUCTURE_32H(0x32);

    override fun toString(): String {
      return "Structure ${Integer.toHexString(key)}h"
    }

    companion object {
      fun findEnumByKey(key: Int): FileStructureEnum? {
        val values = values()
        return values.find { it.key == key }
      }
    }
  }
}
