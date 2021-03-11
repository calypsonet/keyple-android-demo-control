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
package org.calypsonet.keyple.demo.control.ticketing

import org.eclipse.keyple.calypso.transaction.CalypsoSam
import org.eclipse.keyple.calypso.transaction.PoSecuritySettings
import org.eclipse.keyple.core.card.selection.CardResource
import org.eclipse.keyple.core.card.selection.CardSelectionsResult
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.event.AbstractDefaultSelectionsResponse
import org.calypsonet.keyple.demo.control.models.CardReaderResponse
import org.calypsonet.keyple.demo.control.models.Location

interface ITicketingSession {
    val poReader: Reader?
    val samReader: Reader?
    val poTypeName: String?

    fun prepareAndSetPoDefaultSelection()
    fun processDefaultSelection(selectionResponse: AbstractDefaultSelectionsResponse?): CardSelectionsResult
    fun checkStructure(): Boolean
    fun checkStartupInfo(): Boolean
    fun launchControlProcedure(locations: List<Location>): CardReaderResponse
    fun checkSamAndOpenChannel(samReader: Reader): CardResource<CalypsoSam>
    fun getSecuritySettings(samResource: CardResource<CalypsoSam>?): PoSecuritySettings?
}
