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
package org.calypsonet.keyple.demo.control.android.activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_card_reader.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.ApplicationSettings
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.android.di.scope.ActivityScoped
import org.calypsonet.keyple.demo.control.service.ticketing.CalypsoInfo
import org.calypsonet.keyple.demo.control.service.ticketing.TicketingService
import org.calypsonet.keyple.demo.control.service.ticketing.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.service.ticketing.model.Status
import org.calypsonet.terminal.reader.CardReaderEvent
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi
import timber.log.Timber

@ActivityScoped
class ReaderActivity : BaseActivity() {

  @Suppress("DEPRECATION") private lateinit var progress: ProgressDialog
  private var cardReaderObserver: CardReaderObserver? = null
  private lateinit var ticketingService: TicketingService
  var currentAppState = AppState.WAIT_SYSTEM_READY

  // application states
  enum class AppState {
    UNSPECIFIED,
    WAIT_SYSTEM_READY,
    WAIT_CARD,
    CARD_STATUS
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_card_reader)
    setSupportActionBar(findViewById(R.id.toolbar))

    @Suppress("DEPRECATION")
    progress = ProgressDialog(this)
    @Suppress("DEPRECATION")
    progress.setMessage(getString(R.string.please_wait))

    progress.setCancelable(false)
  }

  override fun onResume() {
    super.onResume()
    loadingAnimation.playAnimation()

    if (!mainService.readersInitialized) {
      GlobalScope.launch {
        withContext(Dispatchers.Main) { showProgress() }

        withContext(Dispatchers.IO) {
          try {
            cardReaderObserver = CardReaderObserver()
            mainService.init(
                cardReaderObserver, this@ReaderActivity, ApplicationSettings.readerType)
            ticketingService = mainService.ticketingService!!
            mainService.readersInitialized = true
            showToast(
                getString(
                    if (ticketingService.isSecureSessionMode) R.string.secure_session_mode_enabled
                    else R.string.secure_session_mode_disabled))
            handleAppEvents(AppState.WAIT_CARD, null)
            mainService.startNfcDetection()
          } catch (e: Exception) {
            Timber.e(e)
            withContext(Dispatchers.Main) {
              dismissProgress()
              showNoProxyReaderDialog(e)
            }
          }
        }
        if (mainService.readersInitialized) {
          withContext(Dispatchers.Main) { dismissProgress() }
        }
      }
    } else {
      mainService.startNfcDetection()
    }
  }

  override fun onPause() {
    super.onPause()
    loadingAnimation.cancelAnimation()
    if (mainService.readersInitialized) {
      mainService.stopNfcDetection()
      Timber.d("stopNfcDetection")
    }
  }

  override fun onDestroy() {
    mainService.onDestroy(cardReaderObserver)
    cardReaderObserver = null
    super.onDestroy()
  }

  /**
   * main app state machine handle
   *
   * @param appState
   * @param readerEvent
   */
  private fun handleAppEvents(appState: AppState, readerEvent: CardReaderEvent?) {

    var newAppState = appState

    Timber.i(
        "Current state = $currentAppState, wanted new state = $newAppState, event = ${readerEvent?.type}")
    when (readerEvent?.type) {
      CardReaderEvent.Type.CARD_INSERTED, CardReaderEvent.Type.CARD_MATCHED -> {
        if (newAppState == AppState.WAIT_SYSTEM_READY) {
          return
        }
        Timber.i("Process default selection...")

        val seSelectionResult =
            ticketingService.parseScheduledCardSelectionsResponse(
                readerEvent.scheduledCardSelectionsResponse)

        if (seSelectionResult.activeSelectionIndex == -1) {
          Timber.e("Card Not selected")
          val error = getString(R.string.card_invalid_aid)
          displayResult(
              CardReaderResponse(
                  status = Status.INVALID_CARD, titlesList = arrayListOf(), errorMessage = error))
          return
        }

        Timber.i("Card AID = ${ticketingService.cardAid}")
        if (CalypsoInfo.AID_1TIC_ICA_1 != ticketingService.cardAid &&
            CalypsoInfo.AID_1TIC_ICA_3 != ticketingService.cardAid &&
            CalypsoInfo.AID_NORMALIZED_IDF != ticketingService.cardAid) {
          val error = getString(R.string.card_invalid_aid)
          displayResult(
              CardReaderResponse(
                  status = Status.INVALID_CARD, titlesList = arrayListOf(), errorMessage = error))
          return
        }

        if (!ticketingService.checkStructure()) {
          val error = getString(R.string.card_invalid_structure)
          displayResult(
              CardReaderResponse(
                  status = Status.INVALID_CARD, titlesList = arrayListOf(), errorMessage = error))
          return
        }

        Timber.i("A Calypso Card selection succeeded.")
        newAppState = AppState.CARD_STATUS
      }
      CardReaderEvent.Type.CARD_REMOVED -> {
        currentAppState = AppState.WAIT_SYSTEM_READY
      }
      else -> {
        Timber.w("Event type not handled.")
      }
    }

    when (newAppState) {
      AppState.WAIT_SYSTEM_READY, AppState.WAIT_CARD -> {
        currentAppState = newAppState
      }
      AppState.CARD_STATUS -> {
        currentAppState = newAppState
        when (readerEvent?.type) {
          CardReaderEvent.Type.CARD_INSERTED, CardReaderEvent.Type.CARD_MATCHED -> {
            GlobalScope.launch {
              try {
                // Launch the control procedure
                withContext(Dispatchers.Main) { progress.show() }
                val cardReaderResponse =
                    withContext(Dispatchers.IO) {
                      ticketingService.launchControlProcedure(locationFileService.locations)
                    }
                withContext(Dispatchers.Main) {
                  if (cardReaderResponse.status == Status.EMPTY_CARD ||
                      cardReaderResponse.status == Status.ERROR) {
                    mainService.displayResultFailed()
                  } else {
                    mainService.displayResultSuccess()
                  }
                  progress.dismiss()
                  displayResult(cardReaderResponse)
                }
              } catch (e: IllegalStateException) {
                Timber.e(e)
                Timber.e("Load ERROR page after exception = ${e.message}")
                displayResult(CardReaderResponse(status = Status.ERROR, titlesList = arrayListOf()))
              }
            }
          }
          else -> {
            // Do nothing
          }
        }
      }
      AppState.UNSPECIFIED -> {
        Toast.makeText(this, getString(R.string.status_unspecified), Toast.LENGTH_SHORT).show()
      }
    }
    Timber.i("New state = $currentAppState")
  }

  private fun displayResult(cardReaderResponse: CardReaderResponse?) {
    if (cardReaderResponse != null) {

      runOnUiThread { loadingAnimation.cancelAnimation() }

      when (cardReaderResponse.status) {
        Status.TICKETS_FOUND, Status.EMPTY_CARD -> {
          val intent = Intent(this@ReaderActivity, CardContentActivity::class.java)
          intent.putExtra(CARD_CONTENT, cardReaderResponse)
          startActivity(intent)
        }
        Status.LOADING, Status.ERROR, Status.SUCCESS, Status.INVALID_CARD -> {
          mainService.displayResultFailed()
          val intent = Intent(this@ReaderActivity, NetworkInvalidActivity::class.java)
          intent.putExtra(CARD_CONTENT, cardReaderResponse)
          startActivity(intent)
        }
        Status.WRONG_CARD -> {
          // Do nothing
        }
        Status.DEVICE_CONNECTED -> {
          // Do nothing
        }
      }
    }
  }

  private fun showNoProxyReaderDialog(t: Throwable) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(R.string.error_title)
    builder.setMessage(t.message)
    builder.setNegativeButton(R.string.quit) { _, _ -> finish() }
    val dialog = builder.create()
    dialog.setCancelable(false)
    dialog.show()
  }

  private fun showProgress() {
    if (!progress.isShowing) {
      progress.show()
    }
  }

  private fun dismissProgress() {
    if (progress.isShowing) {
      progress.dismiss()
    }
  }

  companion object {
    const val CARD_CONTENT = "cardContent"
  }

  private inner class CardReaderObserver : CardReaderObserverSpi {

    override fun onReaderEvent(readerEvent: CardReaderEvent?) {
      Timber.i("New ReaderEvent received :${readerEvent?.type?.name}")
      handleAppEvents(currentAppState, readerEvent)
    }
  }
}
