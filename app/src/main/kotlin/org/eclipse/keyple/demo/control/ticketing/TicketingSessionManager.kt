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
package org.eclipse.keyple.demo.control.ticketing

import org.eclipse.keyple.core.service.exception.KeypleReaderException
import org.eclipse.keyple.core.service.exception.KeypleReaderNotFoundException
import org.eclipse.keyple.demo.control.reader.IReaderRepository
import timber.log.Timber

class TicketingSessionManager(private val ticketingSessions: ArrayList<ITicketingSession> = ArrayList()) {

    @Throws(KeypleReaderException::class)
    fun createTicketingSession(
        readerRepository: IReaderRepository,
        explicitSelection: Boolean = false
    ): ITicketingSession {
        val ticketingSession: ITicketingSession
        if (explicitSelection) {
            Timber.d("Create a new TicketingSessionExplicitSelection for reader ${readerRepository.poReader?.name}")
            ticketingSession = TicketingSessionExplicitSelection(readerRepository)
        } else {
            Timber.d("Created a new TicketingSession for reader ${readerRepository.poReader?.name}")
            ticketingSession = TicketingSession(readerRepository)
        }
        ticketingSessions.add(ticketingSession)
        return ticketingSession
    }

    @Throws(KeypleReaderNotFoundException::class)
    fun destroyAll() {
        Timber.d("Destroy all TicketingSession")
        ticketingSessions.clear()
    }

    @Throws(KeypleReaderNotFoundException::class)
    fun destroyTicketingSession(poReaderName: String): Boolean {
        Timber.d("Destroy a the TicketingSession for reader $poReaderName")
        for (ticketingSession in ticketingSessions) {
            if (ticketingSession.poReader?.name == poReaderName) {
                ticketingSessions.remove(ticketingSession)
                Timber.d("Session removed for reader ${ticketingSession.poReader} - $ticketingSession")
                return true
            }
        }
        Timber.d("No TicketingSession found for reader $poReaderName")
        return false
    }

    fun getTicketingSession(poReaderName: String): ITicketingSession? {
        Timber.d("Retrieve the TicketingSession of reader $poReaderName")
        for (ticketingSession in ticketingSessions) {
            if (ticketingSession.poReader?.name == poReaderName) {
                Timber.d("TicketingSession found for reader $poReaderName")
                return ticketingSession
            }
        }
        Timber.d("No TicketingSession found for reader $poReaderName")
        return null
    }

    fun findAll(): List<ITicketingSession> {
        return ticketingSessions
    }
}
