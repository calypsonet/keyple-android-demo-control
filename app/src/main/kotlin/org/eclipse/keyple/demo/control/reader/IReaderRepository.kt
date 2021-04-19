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
package org.eclipse.keyple.demo.control.reader

import android.app.Activity
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.Reader

interface IReaderRepository {

    var poReader: Reader?
    var samReaders: MutableMap<String, Reader>

    fun registerPlugin(activity: Activity)

    suspend fun initPoReader(): Reader?

    suspend fun initSamReaders(): Map<String, Reader>

    fun getSamReader(): Reader?
    fun getContactlessIsoProtocol(): PoReaderProtocol?
    fun getContactlessMifareProtocol(): PoReaderProtocol?
    fun getSamReaderProtocol(): String
    fun clear()

    fun isMockedResponse(): Boolean = false

    fun getPermissions(): Array<String>? = null
}

data class PoReaderProtocol(val readerProtocolName: String, val applicationProtocolName: String)
