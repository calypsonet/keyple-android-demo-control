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

/**
 * Helper class to provide specific elements to handle Calypso cards.
 *
 *  * AID application selection (default Calypso AID)
 *  * SAM_C1_ATR_REGEX regular expression matching the expected C1 SAM ATR
 *  * Files infos (SFI, rec number, etc) for
 *
 *  * Environment and Holder
 *  * Event Log
 *  * Contract List
 *  * Contracts
 *
 *
 *
 */
object CalypsoInfo {
    /*********************************
     *              AIDs
     *********************************/
    /** AID 1TIC.ICA * */
    const val AID_HISTORIC = "315449432e494341"

    /** AID Intercode * */
    const val AID_HIS_STRUCTURE_13H = "315449432e49434132"
    const val AID_HIS_STRUCTURE_5H = "315449432e49434131"
//    const val AID_NAVIGO = "A0000004040125090101"

    /** AID NORMALIZED IDF * */
    const val AID_NORMALIZED_IDF = "A0000004040125090101"

    /** AID BANKING * */
    const val AID_BANKING = "325041592e5359532e4444463031"



    /*********************************
     *           Card types
     *********************************/
    const val PO_TYPE_NAME_CALYPSO = "Calypso"
    const val PO_TYPE_NAME_NAVIGO = "Navigo"
    const val PO_TYPE_NAME_BANKING = "Banking"
    const val PO_TYPE_NAME_OTHER = "Unknown"

    /** Audit-C0  */ // public final static String AID = "315449432E4943414C54";
    // / ** 1TIC.ICA AID */
    // public final static String AID = "315449432E494341";
    /** SAM C1 regular expression: platform, version and serial number values are ignored  */
    const val SAM_C1_ATR_REGEX = "3B3F9600805A[0-9a-fA-F]{2}80.1[0-9a-fA-F]{14}829000"
    const val ATR_REV1_REGEX = "3B8F8001805A0A0103200311........829000.."

    const val RECORD_NUMBER_1: Byte = 1
    const val RECORD_NUMBER_2: Byte = 2
    const val RECORD_NUMBER_3: Byte = 3
    const val RECORD_NUMBER_4: Byte = 4

    const val SFI_EnvironmentAndHolder = 0x07.toByte()
    const val SFI_EventLog = 0x08.toByte()
    const val SFI_ContractList = 0x1E.toByte()
    const val SFI_Contracts = 0x09.toByte()
    const val SFI_Counter = 0x19.toByte()
    const val SFI_Counter_0A = 0x0A.toByte()
    const val SFI_Counter_0B = 0x0B.toByte()
    const val SFI_Counter_0C = 0x0C.toByte()
    const val SFI_Counter_0D = 0x0D.toByte()

    const val eventLog_dataFill =
        "00112233445566778899AABBCCDDEEFF00112233445566778899AABBCC"


    const val DATA_ENV =
        "01 00 00 00 01 0F BF 18 4E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
    const val DATA_CONTRACT_1 =
        "01 01 0F BF 0F DD 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
    const val DATA_EVENT_1 =
        "01 0F BF 03 48 00 00 00 01 01 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
}
