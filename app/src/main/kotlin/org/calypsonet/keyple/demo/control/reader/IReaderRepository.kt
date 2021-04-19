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
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi

interface IReaderRepository {

    var poReader: Reader?
    var samReaders: MutableList<Reader>

    fun registerPlugin(activity: Activity)

    suspend fun initPoReader(): Reader?

    suspend fun initSamReaders(): List<Reader>

    fun getSamReader(): Reader?
    fun getContactlessIsoProtocol(): PoReaderProtocol?
    fun getSamReaderProtocol(): String?
    fun clear()
    fun getPlugin(): Plugin
    fun getSamRegex(): String?
    fun getReaderConfiguratorSpi(): ReaderConfiguratorSpi?

    fun isMockedResponse(): Boolean = false

    fun getPermissions(): Array<String>? = null

    /**
     * Method to update color and sound at runtime if needed
     */
    fun displayWaiting(): Boolean = false

    /**
     * Method to update color and sound at runtime if needed
     */
    fun displayResultSuccess(): Boolean = false

    /**
     * Method to update color and sound at runtime if needed
     */
    fun displayResultFailed(): Boolean = false
}

data class PoReaderProtocol(val readerProtocolName: String, val applicationProtocolName: String)
