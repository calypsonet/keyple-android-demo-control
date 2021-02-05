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

import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.exception.KeypleReaderException

interface ITicketingSession {
    val poReader: Reader?
    val cardContent: CardContent
    val poTypeName: String?
    fun analyzePoProfile(): Boolean
    val poIdentification: String?

    @Throws(KeypleReaderException::class)
    fun loadTickets(ticketNumber: Int): Int
    fun notifySeProcessed()

    companion object {
        const val STATUS_OK = 0
        const val STATUS_UNKNOWN_ERROR = 1
        const val STATUS_CARD_SWITCHED = 2
        const val STATUS_SESSION_ERROR = 3
    }
}
