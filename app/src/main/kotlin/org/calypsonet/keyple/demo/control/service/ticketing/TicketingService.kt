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
package org.calypsonet.keyple.demo.control.service.ticketing

import java.util.*
import javax.inject.Inject
import org.calypsonet.keyple.demo.control.android.di.scope.AppScoped
import org.calypsonet.keyple.demo.control.service.reader.ReaderService
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.AID_1TIC_ICA_1
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.AID_1TIC_ICA_3
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.AID_NORMALIZED_IDF
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.AID_OTHER
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.DEFAULT_KIF_DEBIT
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.DEFAULT_KIF_LOAD
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo.DEFAULT_KIF_PERSONALIZATION
import org.calypsonet.keyple.demo.control.service.ticketing.exception.ControlException
import org.calypsonet.keyple.demo.control.service.ticketing.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.service.ticketing.model.FileStructureEnum
import org.calypsonet.keyple.demo.control.service.ticketing.model.Location
import org.calypsonet.keyple.demo.control.service.ticketing.procedure.ControlProcedure
import org.calypsonet.terminal.calypso.WriteAccessLevel
import org.calypsonet.terminal.calypso.card.CalypsoCard
import org.calypsonet.terminal.calypso.sam.CalypsoSam
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting
import org.calypsonet.terminal.reader.CardReader
import org.calypsonet.terminal.reader.ObservableCardReader
import org.calypsonet.terminal.reader.selection.CardSelectionManager
import org.calypsonet.terminal.reader.selection.CardSelectionResult
import org.calypsonet.terminal.reader.selection.ScheduledCardSelectionsResponse
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.joda.time.DateTime
import timber.log.Timber

@AppScoped
class TicketingService @Inject constructor(private val readerService: ReaderService) {

  private val calypsoExtensionService: CalypsoExtensionService =
      CalypsoExtensionService.getInstance()

  val isSecureSessionMode: Boolean

  private lateinit var calypsoSam: CalypsoSam
  private lateinit var calypsoCard: CalypsoCard
  private lateinit var cardSelectionManager: CardSelectionManager

  private var cardAid: String? = null

  private var indexOfCardSelectionAid1TicIca1 = 0
  private var indexOfCardSelectionAid1TicIca3 = 0
  private var indexOfCardSelectionAidIdf = 0

  private var fileStructure: FileStructureEnum? = null

  private val allowedFileStructures: EnumMap<FileStructureEnum, List<String>> =
      EnumMap(FileStructureEnum::class.java)

  init {
    allowedFileStructures[FileStructureEnum.FILE_STRUCTURE_02H] = listOf(AID_1TIC_ICA_1)
    allowedFileStructures[FileStructureEnum.FILE_STRUCTURE_05H] =
        listOf(AID_1TIC_ICA_1, AID_NORMALIZED_IDF)
    allowedFileStructures[FileStructureEnum.FILE_STRUCTURE_32H] = listOf(AID_1TIC_ICA_3)

    // attempts to select a SAM if any, sets the isSecureSessionMode flag accordingly
    val samReader = readerService.getSamReader()
    isSecureSessionMode = samReader != null && selectSam(samReader)
  }

  fun getCardAid(): String? {
    return cardAid
  }

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
                .filterByDfName(AID_1TIC_ICA_1)
                .filterByCardProtocol(readerService.getCardReaderProtocolLogicalName()))

    // Prepare card selection case #2: 1 TIC ICA 3
    indexOfCardSelectionAid1TicIca3 =
        cardSelectionManager.prepareSelection(
            calypsoExtensionService
                .createCardSelection()
                .filterByDfName(AID_1TIC_ICA_3)
                .filterByCardProtocol(readerService.getCardReaderProtocolLogicalName()))

    // Prepare card selection case #3: Navigo
    indexOfCardSelectionAidIdf =
        cardSelectionManager.prepareSelection(
            calypsoExtensionService
                .createCardSelection()
                .filterByDfName(AID_NORMALIZED_IDF)
                .filterByCardProtocol(readerService.getCardReaderProtocolLogicalName()))

    // Schedule the execution of the prepared card selection scenario as soon as a card is presented
    cardSelectionManager.scheduleCardSelectionScenario(
        readerService.getCardReader() as ObservableCardReader,
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
          cardAid = AID_1TIC_ICA_1
        }
        indexOfCardSelectionAid1TicIca3 -> {
          calypsoCard = cardSelectionResult.activeSmartCard as CalypsoCard
          cardAid = AID_1TIC_ICA_3
          fileStructure = FileStructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
        }
        indexOfCardSelectionAidIdf -> {
          calypsoCard = cardSelectionResult.activeSmartCard as CalypsoCard
          cardAid = AID_NORMALIZED_IDF
          fileStructure = FileStructureEnum.findEnumByKey(calypsoCard.applicationSubtype.toInt())
        }
        else -> cardAid = AID_OTHER
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

  @Throws(ControlException::class)
  fun launchControlProcedure(locations: List<Location>): CardReaderResponse {
    return ControlProcedure()
        .launch(
            cardReader = readerService.getCardReader()!!,
            calypsoCard = calypsoCard,
            cardSecuritySettings = if (isSecureSessionMode) getSecuritySettings() else null,
            locations = locations,
            now = DateTime.now())
  }

  private fun getSecuritySettings(): CardSecuritySetting? {
    return calypsoExtensionService
        .createCardSecuritySetting()
        .setControlSamResource(readerService.getSamReader(), calypsoSam)
        .assignDefaultKif(WriteAccessLevel.PERSONALIZATION, DEFAULT_KIF_PERSONALIZATION)
        .assignDefaultKif(WriteAccessLevel.LOAD, DEFAULT_KIF_LOAD)
        .assignDefaultKif(WriteAccessLevel.DEBIT, DEFAULT_KIF_DEBIT)
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
}
