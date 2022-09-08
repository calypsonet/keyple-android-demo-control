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
package org.calypsonet.keyple.demo.control.reader

import android.app.Activity
import org.calypsonet.terminal.reader.CardReader
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi

interface ReaderRepository {

  fun registerPlugin(activity: Activity)

  suspend fun initCardReader(): CardReader?
  fun getCardReader(): CardReader?
  fun getCardReaderProtocol(): CardReaderProtocol?

  suspend fun initSamReaders(): List<CardReader>
  fun getSamPlugin(): Plugin
  fun getSamReader(): CardReader?
  fun getSamReaderNameRegex(): String?
  fun getSamReaderConfiguratorSpi(): ReaderConfiguratorSpi?
  fun getSamPermissions(): Array<String>? = null

  fun clear()

  /** Method to update color and sound at runtime if needed */
  fun displayWaiting(): Boolean = false

  /** Method to update color and sound at runtime if needed */
  fun displayResultSuccess(): Boolean = false

  /** Method to update color and sound at runtime if needed */
  fun displayResultFailed(): Boolean = false
}
