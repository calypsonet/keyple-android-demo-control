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
package org.eclipse.keyple.demo.control.data

import android.app.Activity
import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardService
import org.eclipse.keyple.core.service.event.ObservableReader
import org.eclipse.keyple.core.service.exception.KeypleException
import org.eclipse.keyple.core.service.exception.KeyplePluginInstantiationException
import org.eclipse.keyple.core.service.exception.KeyplePluginNotFoundException
import org.eclipse.keyple.core.service.exception.KeypleReaderIOException
import org.eclipse.keyple.demo.control.di.scopes.AppScoped
import org.eclipse.keyple.demo.control.reader.IReaderRepository
import org.eclipse.keyple.demo.control.ticketing.CardContent
import org.eclipse.keyple.demo.control.ticketing.TicketingSession
import org.eclipse.keyple.demo.control.ticketing.TicketingSessionManager
import org.eclipse.keyple.demo.control.utils.CardletUtils
import org.eclipse.keyple.parser.dto.CardletInputDto
import org.eclipse.keyple.parser.model.CardletDto
import timber.log.Timber
import javax.inject.Inject

@AppScoped
class CardReaderApi @Inject constructor(
    private var readerRepository: IReaderRepository
) {

    private lateinit var ticketingSessionManager: TicketingSessionManager
    private var ticketingSession: TicketingSession? = null

    @Throws(
        KeyplePluginInstantiationException::class,
        IllegalStateException::class,
        KeyplePluginNotFoundException::class
    )
    suspend fun init(observer: ObservableReader.ReaderObserver?, activity: Activity) {
        /*
         * Register plugin
         */
        try {
            readerRepository.registerPlugin(activity)
        } catch (e: KeypleException) {
            Timber.e(e)
            throw IllegalStateException(e.message)
        }

        /*
         * Init PO reader
         */
        val poReader: Reader?
        try {
            poReader = readerRepository.initPoReader()
        } catch (e: KeyplePluginNotFoundException) {
            Timber.e(e)
            throw IllegalStateException("PoReader with name AndroidCoppernicAskPlugin was not found")
        } catch (e: KeypleReaderIOException) {
            Timber.e(e)
            throw IllegalStateException(e.message)
        } catch (e: KeypleException) {
            Timber.e(e)
            throw IllegalStateException(e.message)
        }

        /*
         * Init SAM reader
         */
        var samReaders: Map<String, Reader>? = null
        try {
            samReaders = readerRepository.initSamReaders()
        } catch (e: KeyplePluginNotFoundException) {
            Timber.e(e)
        }
        if (samReaders.isNullOrEmpty()) {
            Timber.w("No SAM reader available")
        }

        poReader.let { reader ->
            /* remove the observer if it already exist */
            (reader as ObservableReader).addObserver(observer)

            ticketingSessionManager = TicketingSessionManager()

            ticketingSession =
                ticketingSessionManager.createTicketingSession(readerRepository) as TicketingSession
        }
    }

    fun startNfcDetection() {
        /*
        * Provide the Reader with the selection operation to be processed when a PO is
        * inserted.
        */
        ticketingSession?.prepareAndSetPoDefaultSelection()

        (readerRepository.poReader as ObservableReader).startCardDetection(ObservableReader.PollingMode.REPEATING)
    }

    fun stopNfcDetection() {
        try {
            // notify reader that se detection has been switched off
            (readerRepository.poReader as ObservableReader).stopCardDetection()
        } catch (e: KeyplePluginNotFoundException) {
            Timber.e(e, "NFC Plugin not found")
        }
    }

    fun getTicketingSession(): TicketingSession? {
        return ticketingSession
    }

    fun onDestroy(observer: ObservableReader.ReaderObserver?) {
        readerRepository.clear()
        if (observer != null && readerRepository.poReader != null) {
            (readerRepository.poReader as ObservableReader).removeObserver(observer)
        }

        SmartCardService.getInstance().plugins.forEach {
            SmartCardService.getInstance().unregisterPlugin(it.key)
        }

        ticketingSession = null
    }

    fun isMockedResponse(): Boolean {
        return readerRepository.isMockedResponse()
    }

    fun parseCardlet(): CardletDto? {
        if (ticketingSession == null) {
            Timber.w("No ticketing session available")
            return null
        }

        val cardContent = ticketingSession!!.cardContent
        printCardContent(cardContent)

        var env: ByteArray = byteArrayOf()
        cardContent.environment.forEach {
            env = it.value
        }

        val contracts = mutableListOf<ByteArray>()
        cardContent.contracts.forEach {
            contracts.add(it.value)
        }

        val events = mutableListOf<ByteArray>()
        cardContent.eventLog.forEach {
            events.add(it.value)
        }

        val counter = cardContent.counters[0] ?: CardletUtils.getEmptyFile()

        val cardletInputDto = CardletInputDto(
            envData = env,
            contractData = contracts,
            eventData = events,
            counterData = counter
        )

        return ticketingSession?.parseCardlet(cardletInputDto)
    }

    private fun printCardContent(cardContent: CardContent){
        println(">>> ")
        println(">>> ")
        println(">>> CardReaderApi.printCardContent - DISPLAY CONTENT")
        println(">>> ")
        println(">>> CardReaderApi.printCardContent - ENV")
        var env: ByteArray = ByteArray(4)
        cardContent.environment.forEach {
            println(">>> CardReaderApi.printCardContent - ${it.key} : ${getSfiContent(it.value)}")
            env = it.value
        }
        println(">>> CardReaderApi.printCardContent - CONTRACT LIST")
        var contractsList: ByteArray = ByteArray(4)
        cardContent.contractsList.forEach {
            println(">>> CardReaderApi.printCardContent - ${it.key} : ${getSfiContent(it.value)}")
            contractsList = it.value
        }
        println(">>> ")
        println(">>> CardReaderApi.printCardContent - CONTRACTS")
        val contracts = mutableListOf<ByteArray>()
        cardContent.contracts.forEach {
            println(">>> CardReaderApi.printCardContent - ${it.key} : ${getSfiContent(it.value)}")
            contracts.add(it.value)
        }
        println(">>> ")
        println(">>> CardReaderApi.printCardContent - EVENT")
        val events = mutableListOf<ByteArray>()
        cardContent.eventLog.forEach {
            println(">>> CardReaderApi.printCardContent - ${it.key} : ${getSfiContent(it.value)}")
            events.add(it.value)
        }
        println(">>> ")
        println(">>> ")
    }

    private fun getSfiContent(content: ByteArray?): String = BytesUtils.bytesToString(
        content ?: byteArrayOf(0))
}
