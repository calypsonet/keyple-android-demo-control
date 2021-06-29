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
package org.calypsonet.keyple.demo.control.reader

import android.app.Activity
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.exception.KeypleException

interface IReaderRepository {

    var poReader: Reader?
    var samReaders: MutableMap<String, Reader>

    @Throws(KeypleException::class)
    fun registerPlugin(activity: Activity)

    @Throws(KeypleException::class)
    suspend fun initPoReader(): Reader?

    @Throws(KeypleException::class)
    suspend fun initSamReaders(): Map<String, Reader>

    fun getSamReader(): Reader?
    fun getContactlessIsoProtocol(): PoReaderProtocol?
    fun getSamReaderProtocol(): String?
    fun clear()

    fun isMockedResponse(): Boolean = false

    fun getPermissions(): Array<String>? = null

    /**
     * Method used only for Flowbird device. Changes color and sound if needed
     */
    fun displayWaiting(): Boolean = false

    /**
     * Method used only for Flowbird device. Changes color and sound if needed
     */
    fun displayResultSuccess(): Boolean = false

    /**
     * Method used only for Flowbird device. Changes color and sound if needed
     */
    fun displayResultFailed(): Boolean = false
}

data class PoReaderProtocol(val readerProtocolName: String, val applicationProtocolName: String)
