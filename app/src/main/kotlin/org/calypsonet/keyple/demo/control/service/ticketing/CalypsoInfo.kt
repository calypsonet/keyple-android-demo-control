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
package org.calypsonet.keyple.demo.control.service.ticketing

/**
 * Helper class to provide specific elements to handle Calypso cards.
 *
 * * AID application selection (default Calypso AID)
 * * SAM_C1_ATR_REGEX regular expression matching the expected C1 SAM ATR
 * * Files info (SFI, rec number, etc)
 */
object CalypsoInfo {

  const val AID_1TIC_ICA_1 = "315449432e49434131"
  const val AID_1TIC_ICA_3 = "315449432E49434133"
  const val AID_NORMALIZED_IDF = "A0000004040125090101"
  const val AID_OTHER = "Other"

  const val RECORD_NUMBER_1: Byte = 1
  const val RECORD_NUMBER_4: Byte = 4

  const val SFI_ENVIRONMENT_AND_HOLDER = 0x07.toByte()
  const val SFI_EVENTS_LOG = 0x08.toByte()
  const val SFI_CONTRACTS = 0x09.toByte()
  const val SFI_COUNTER = 0x19.toByte()

  const val SAM_PROFILE_NAME = "SAM C1"

  const val DEFAULT_KIF_PERSONALIZATION = 0x21.toByte()
  const val DEFAULT_KIF_LOAD = 0x27.toByte()
  const val DEFAULT_KIF_DEBIT = 0x30.toByte()
}
