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
package org.calypsonet.keyple.demo.control.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_card_reader.loadingAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.di.scopes.ActivityScoped
import org.calypsonet.keyple.demo.control.models.CardReaderResponse
import org.calypsonet.keyple.demo.control.models.Status
import org.calypsonet.keyple.demo.control.ticketing.CalypsoInfo
import org.calypsonet.keyple.demo.control.ticketing.TicketingSession
import org.calypsonet.terminal.reader.CardReaderEvent
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi
import timber.log.Timber

@ActivityScoped
class CardReaderActivity : BaseActivity() {

  @Suppress("DEPRECATION") private lateinit var progress: ProgressDialog

  private var cardReaderObserver: CardReaderObserver? = null

  private lateinit var ticketingSession: TicketingSession

  var currentAppState = AppState.WAIT_SYSTEM_READY

  /* application states */
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

  override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == android.R.id.home) {
      finish()
    }

    return super.onOptionsItemSelected(menuItem)
  }

  override fun onResume() {
    super.onResume()
    loadingAnimation.playAnimation()

    if (!cardReaderApi.readersInitialized) {
      GlobalScope.launch {
        withContext(Dispatchers.Main) { showProgress() }

        withContext(Dispatchers.IO) {
          try {
            cardReaderObserver = CardReaderObserver()
            cardReaderApi.init(cardReaderObserver, this@CardReaderActivity)
            ticketingSession = cardReaderApi.getTicketingSession()!!
            cardReaderApi.readersInitialized = true
            handleAppEvents(AppState.WAIT_CARD, null)
            cardReaderApi.startNfcDetection()
          } catch (e: Exception) {
            Timber.e(e)
            withContext(Dispatchers.Main) {
              dismissProgress()
              showNoProxyReaderDialog(e)
            }
          }
        }
        if (cardReaderApi.readersInitialized) {
          withContext(Dispatchers.Main) { dismissProgress() }
        }
      }
    } else {
      cardReaderApi.startNfcDetection()
    }
  }

  override fun onPause() {
    super.onPause()
    loadingAnimation.cancelAnimation()
    if (cardReaderApi.readersInitialized) {
      cardReaderApi.stopNfcDetection()
      Timber.d("stopNfcDetection")
    }
  }

  override fun onDestroy() {
    cardReaderApi.onDestroy(cardReaderObserver)
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
            ticketingSession.processDefaultSelection(readerEvent.scheduledCardSelectionsResponse)

        if (seSelectionResult.activeSelectionIndex == -1) {
          Timber.e("Card Not selected")
          val error = getString(R.string.card_invalid_aid)
          displayResult(
              CardReaderResponse(
                  status = Status.INVALID_CARD, titlesList = arrayListOf(), errorMessage = error))
          return
        }

        Timber.i("Card AID = ${ticketingSession.getCardAid()}")
        if (CalypsoInfo.AID_1TIC_ICA_1 != ticketingSession.getCardAid() &&
            CalypsoInfo.AID_1TIC_ICA_3 != ticketingSession.getCardAid() &&
            CalypsoInfo.AID_NORMALIZED_IDF != ticketingSession.getCardAid()) {
          val error = getString(R.string.card_invalid_aid)
          displayResult(
              CardReaderResponse(
                  status = Status.INVALID_CARD, titlesList = arrayListOf(), errorMessage = error))
          return
        }

        if (!ticketingSession.checkStructure()) {
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

                if (ticketingSession.checkStartupInfo()) {
                  /*
                   * LAUNCH CONTROL PROCEDURE
                   */
                  withContext(Dispatchers.Main) { progress.show() }
                  val cardReaderResponse =
                      withContext(Dispatchers.IO) {
                        ticketingSession.launchControlProcedure(locationFileManager.getLocations())
                      }
                  withContext(Dispatchers.Main) {
                    if (cardReaderResponse.status == Status.EMPTY_CARD ||
                        cardReaderResponse.status == Status.ERROR) {
                      cardReaderApi.displayResultFailed()
                    } else {
                      cardReaderApi.displayResultSuccess()
                    }
                    progress.dismiss()
                    displayResult(cardReaderResponse)
                  }
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
          val intent = Intent(this@CardReaderActivity, CardContentActivity::class.java)
          intent.putExtra(CARD_CONTENT, cardReaderResponse)
          startActivity(intent)
        }
        Status.LOADING, Status.ERROR, Status.SUCCESS, Status.INVALID_CARD -> {
          cardReaderApi.displayResultFailed()
          val intent = Intent(this@CardReaderActivity, NetworkInvalidActivity::class.java)
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
