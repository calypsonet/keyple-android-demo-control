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
package org.calypsonet.keyple.demo.control.ui

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
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.AppSettings
import org.calypsonet.keyple.demo.control.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.data.model.Status
import org.calypsonet.keyple.demo.control.di.scope.ActivityScoped
import org.calypsonet.keyple.demo.control.ui.cardcontent.CardContentActivity
import org.calypsonet.terminal.reader.CardReaderEvent
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi
import timber.log.Timber

@ActivityScoped
class ReaderActivity : BaseActivity() {

  @Suppress("DEPRECATION") private lateinit var progress: ProgressDialog
  private var cardReaderObserver: CardReaderObserver? = null
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
    if (!ticketingService.readersInitialized) {
      GlobalScope.launch {
        withContext(Dispatchers.Main) { showProgress() }
        withContext(Dispatchers.IO) {
          try {
            cardReaderObserver = CardReaderObserver()
            ticketingService.init(cardReaderObserver, this@ReaderActivity, AppSettings.readerType)
            showToast(
                getString(
                    if (ticketingService.isSecureSessionMode) R.string.secure_session_mode_enabled
                    else R.string.secure_session_mode_disabled))
            handleAppEvents(AppState.WAIT_CARD, null)
            ticketingService.startNfcDetection()
          } catch (e: Exception) {
            Timber.e(e)
            withContext(Dispatchers.Main) {
              dismissProgress()
              showNoProxyReaderDialog(e)
            }
          }
        }
        if (ticketingService.readersInitialized) {
          withContext(Dispatchers.Main) { dismissProgress() }
        }
      }
    } else {
      ticketingService.startNfcDetection()
    }
  }

  override fun onPause() {
    super.onPause()
    loadingAnimation.cancelAnimation()
    if (ticketingService.readersInitialized) {
      ticketingService.stopNfcDetection()
      Timber.d("stopNfcDetection")
    }
  }

  override fun onDestroy() {
    ticketingService.onDestroy(cardReaderObserver)
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
        Timber.i("Process the selection result...")
        val error =
            ticketingService.analyseSelectionResult(readerEvent.scheduledCardSelectionsResponse)
        if (error != null) {
          Timber.e("Card not selected: %s", error)
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
                      ticketingService.executeControlProcedure(locationRepository.locations)
                    }
                withContext(Dispatchers.Main) {
                  if (cardReaderResponse.status == Status.EMPTY_CARD ||
                      cardReaderResponse.status == Status.ERROR) {
                    ticketingService.displayResultFailed()
                  } else {
                    ticketingService.displayResultSuccess()
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
          ticketingService.displayResultFailed()
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
      Timber.i("New ReaderEvent received: ${readerEvent?.type?.name}")
      handleAppEvents(currentAppState, readerEvent)
    }
  }
}
