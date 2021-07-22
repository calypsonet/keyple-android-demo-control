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

/**
 * Helper class to provide specific elements to handle Calypso cards.
 *
 *  * AID application selection (default Calypso AID)
 *  * SAM_C1_ATR_REGEX regular expression matching the expected C1 SAM ATR
 *  * Files infos (SFI, rec number, etc)
 *
 */
object CalypsoInfo {

    /*********************************
     *              AIDs
     *********************************/

    /** AID Intercode **/
    const val AID_HIS_STRUCTURE_5H_2H = "315449432e49434131"
    const val AID_HIS_STRUCTURE_32H = "315449432E49434133"

    /** AID NORMALIZED IDF **/
    const val AID_NORMALIZED_IDF_05H = "A0000004040125090101"

    /*********************************
     *           Card types and infos
     *********************************/

    const val PO_TYPE_NAME_CALYPSO_02h = "Calypso_02h"
    const val PO_TYPE_NAME_CALYPSO_05h = "Calypso_05h"
    const val PO_TYPE_NAME_CALYPSO_32h = "Calypso_32h"
    const val PO_TYPE_NAME_NAVIGO_05h = "Navigo_05h"
    const val PO_TYPE_NAME_OTHER = "Unknown"

    const val RECORD_NUMBER_1: Byte = 1
    const val RECORD_NUMBER_2: Byte = 2
    const val RECORD_NUMBER_3: Byte = 3
    const val RECORD_NUMBER_4: Byte = 4

    const val SFI_EnvironmentAndHolder = 0x07.toByte()
    const val SFI_EventLog = 0x08.toByte()
    const val SFI_Contracts = 0x09.toByte()
    const val SFI_Counter_0A = 0x0A.toByte()
    const val SFI_Counter_0B = 0x0B.toByte()
    const val SFI_Counter_0C = 0x0C.toByte()
    const val SFI_Counter_0D = 0x0D.toByte()

    /*********************************
     *      Security Settings
     *********************************/

    const val SAM_PROFILE_NAME = "SAM C1"

    /*
     * The default KIF values for personalization, loading and debiting
     */
    const val DEFAULT_KIF_PERSO = 0x21.toByte()
    const val DEFAULT_KIF_LOAD = 0x27.toByte()
    const val DEFAULT_KIF_DEBIT = 0x30.toByte()
}
