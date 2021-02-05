/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.keyple.demo.control.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_card_reader.invalid
import kotlinx.android.synthetic.main.activity_card_reader.loadingAnimation
import kotlinx.android.synthetic.main.activity_card_reader.valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.keyple.core.service.event.ObservableReader
import org.eclipse.keyple.core.service.event.ReaderEvent
import org.eclipse.keyple.core.service.exception.KeyplePluginInstantiationException
import org.eclipse.keyple.demo.control.R
import org.eclipse.keyple.demo.control.data.CardReaderApi
import org.eclipse.keyple.demo.control.di.scopes.ActivityScoped
import org.eclipse.keyple.demo.control.mock.MockUtils
import org.eclipse.keyple.demo.control.models.CardReaderResponse
import org.eclipse.keyple.demo.control.models.Status
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo
import org.eclipse.keyple.demo.control.ticketing.TicketingSession
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject


@ActivityScoped
class CardReaderActivity : BaseActivity() {

    private lateinit var timer: Timer

    private lateinit var progress: ProgressDialog

    @Inject
    lateinit var cardReaderApi: CardReaderApi

    private var poReaderObserver: PoObserver? = null
    private var readersInitialized = false
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

        progress = ProgressDialog(this)
        progress.setMessage(getString(R.string.please_wait))
        progress.setCancelable(false)

        /*
         * Use cases (for test purposes)
         */
        valid.setOnClickListener {
            val intent = Intent(this@CardReaderActivity, CardContentActivity::class.java)
            val cardResponse = MockUtils.getMockedResult(this@CardReaderActivity, Status.TICKETS_FOUND)
            intent.putExtra(CARD_CONTENT, cardResponse)
            startActivity(intent)
        }
        invalid.setOnClickListener {
            val intent = Intent(this, NetworkInvalidActivity::class.java)
            val cardResponse =
                MockUtils.getMockedResult(this@CardReaderActivity, Status.INVALID_CARD)
            intent.putExtra(CARD_CONTENT, cardResponse)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadingAnimation.playAnimation()

        if (!readersInitialized) {
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    showProgress()
                }

                withContext(Dispatchers.IO) {
                    try {
                        poReaderObserver = PoObserver()
                        cardReaderApi.init(poReaderObserver, this@CardReaderActivity)
                        ticketingSession = cardReaderApi.getTicketingSession()!!
                        readersInitialized = true
                        handleAppEvents(AppState.WAIT_CARD, null)
                        cardReaderApi.startNfcDetection()
                    } catch (e: KeyplePluginInstantiationException) {
                        Timber.e(e)
                        withContext(Dispatchers.Main) {
                            dismissProgress()
                            showNoProxyReaderDialog(e)
                        }
                    } catch (e: IllegalStateException) {
                        Timber.e(e)
                        withContext(Dispatchers.Main) {
                            dismissProgress()
                            showNoProxyReaderDialog(e)
                        }
                    }
                }
                if (readersInitialized) {
                    withContext(Dispatchers.Main) {
                        dismissProgress()
                    }
                }
            }
        } else {
            cardReaderApi.startNfcDetection()
        }

        timer = Timer() // After cancel, need to reinit Timer
//        timer.schedule(object : TimerTask() {
//            override fun run() {
//                runOnUiThread { onBackPressed() }
//            }
//        }, RETURN_DELAY_MS.toLong())
    }

    override fun onPause() {
        super.onPause()
        loadingAnimation.cancelAnimation()
        timer.cancel()
        if (readersInitialized) {
            cardReaderApi.stopNfcDetection()
            Timber.d("stopNfcDetection")
        }
    }

    override fun onDestroy() {
        readersInitialized = false
        cardReaderApi.onDestroy(poReaderObserver)
        poReaderObserver = null
        super.onDestroy()
    }

    /**
     * main app state machine handle
     *
     * @param appState
     * @param readerEvent
     */
    private fun handleAppEvents(appState: AppState, readerEvent: ReaderEvent?) {

        var newAppState = appState

        Timber.i("Current state = $currentAppState, wanted new state = $newAppState, event = ${readerEvent?.eventType}")
        when (readerEvent?.eventType) {
            ReaderEvent.EventType.CARD_INSERTED, ReaderEvent.EventType.CARD_MATCHED -> {
                if (newAppState == AppState.WAIT_SYSTEM_READY) {
                    return
                }
                Timber.i("Process default selection...")

                val seSelectionResult =
                    ticketingSession.processDefaultSelection(readerEvent.defaultSelectionsResponse)

                if (!seSelectionResult.hasActiveSelection()) {
                    Timber.e("PO Not selected")
                    val error = String.format(
                        getString(R.string.card_invalid_desc),
                        "a case of PO Not selected"
                    )
                    displayResult(
                        CardReaderResponse(
                            status = Status.INVALID_CARD,
                            cardType = null,
                            lastValidationsList = arrayListOf(),
                            titlesList = arrayListOf(),
                            errorMessage = error
                        )
                    )
                    return
                }

                Timber.i("PO Type = ${ticketingSession.poTypeName}")
                if (CalypsoInfo.PO_TYPE_NAME_CALYPSO != ticketingSession.poTypeName) {
                    val cardType = ticketingSession.poTypeName ?: "an unknown card"
                    val error = String.format(
                        getString(R.string.card_invalid_desc),
                        cardType.trim { it <= ' ' }
                    )
                    displayResult(
                        CardReaderResponse(
                            status = Status.INVALID_CARD,
                            cardType = cardType,
                            titlesList = arrayListOf(),
                            lastValidationsList = arrayListOf(),
                            errorMessage = error
                        )
                    )
                    return
                } else {
                    Timber.i("A Calypso PO selection succeeded.")
                    newAppState = AppState.CARD_STATUS
                }
            }
            ReaderEvent.EventType.CARD_REMOVED -> {
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
                when (readerEvent?.eventType) {
                    ReaderEvent.EventType.CARD_INSERTED, ReaderEvent.EventType.CARD_MATCHED -> {
                        try {

                            if (ticketingSession.analyzePoProfile()) {
                                /*
                                 * LAUNCH VALIDATION PROCEDURE
                                 */
                                val cardReaderResponse =
                                    ticketingSession.launchControlProcedure(locationFileManager.getLocations())
                                displayResult(cardReaderResponse)

                                /*
                                 * LAUNCH PERSONALIZE PROCEDURE
                                 */
//                                val success = ticketingSession.launchPersonalizeProcedure(
//                                    ContractPriorityEnum.SEASON_PASS)
//                                val message = if(success == Status.SUCCESS){
//                                    "Personalize successful"
//                                }
//                                else{
//                                    "Personalize error !"
//                                }
//                                runOnUiThread {
//                                    Toast.makeText(this@CardReaderActivity, message, Toast.LENGTH_SHORT).show()
//                                }
                            }
                        } catch (e: IllegalStateException) {
                            Timber.e(e)
                            Timber.e("Load ERROR page after exception = ${e.message}")
                            displayResult(
                                CardReaderResponse(
                                    Status.ERROR,
                                    "some invalid card",
                                    arrayListOf(),
                                    arrayListOf()
                                )
                            )
                        }
                    }
                    else -> {
                        //Do nothing
                    }
                }
            }
            AppState.UNSPECIFIED -> {
                Toast.makeText(this, getString(R.string.status_unspecified), Toast.LENGTH_SHORT)
                    .show()
            }
        }
        Timber.i("New state = $currentAppState")
    }

    /**
     * Used to mock card responses -> display chosen result screen
     */
    private fun launchMockedEvents() {
        Timber.i("Launch STUB Card event !!")
        // STUB Card event
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                /*
                 * Change this value to see other status screens
                 */
                val status: Status = Status.TICKETS_FOUND

                val cardReaderResponse = MockUtils.getMockedResult(this@CardReaderActivity, status)
                if (cardReaderResponse != null) {
                    displayResult(cardReaderResponse)
                }
            }
        }, 1000)
    }

    private fun displayResult(cardReaderResponse: CardReaderResponse?) {
        if (cardReaderResponse != null) {

            runOnUiThread {
                loadingAnimation.cancelAnimation()
            }

            when (cardReaderResponse.status) {
                Status.TICKETS_FOUND -> {
                    val intent = Intent(this@CardReaderActivity, CardContentActivity::class.java)
                    intent.putExtra(CARD_CONTENT, cardReaderResponse)
                    startActivity(intent)
                }
                Status.LOADING, Status.ERROR, Status.SUCCESS, Status.INVALID_CARD, Status.EMPTY_CARD -> {
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
        builder.setNegativeButton(R.string.quit) { _, _ ->
            finish()
        }
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

    private inner class PoObserver : ObservableReader.ReaderObserver {
        override fun update(event: ReaderEvent) {
            Timber.i("New ReaderEvent received :${event.eventType.name}")
            if (event.eventType == ReaderEvent.EventType.CARD_MATCHED &&
                cardReaderApi.isMockedResponse()
            ) {
                launchMockedEvents()
            } else {
                handleAppEvents(currentAppState, event)
            }
        }
    }
}
