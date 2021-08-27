/********************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.calypsonet.keyple.parser.intercode.parser

import fr.devnied.bitlib.BitUtils
import fr.devnied.bitlib.BytesUtils
import org.calypsonet.keyple.parser.IParser
import org.calypsonet.keyple.parser.IParser.Companion.DATE_01_01_1997
import org.calypsonet.keyple.parser.intercode.model.EventValidation
import org.calypsonet.keyple.parser.intercode.model.IntercodeEventTransportEnum
import org.calypsonet.keyple.parser.intercode.model.IntercodeEventTypeEnum
import org.calypsonet.keyple.parser.intercode.utils.PlaceUtils

class IntercodeEventParser : IParser<EventValidation> {

    override fun parse(content: ByteArray): EventValidation {

        var name = ""
        var location: String? = null
        var destination: String? = null

        val bitUtils = BitUtils(content)
        bitUtils.currentBitIndex = 0

        var eventDate = parseDate(bitUtils.getNextInteger(EVENT_DATE_STAMP_SIZE), DATE_01_01_1997)
        eventDate = parseMinutes(eventDate, bitUtils.getNextInteger(EVENT_TIME_STAMP_SIZE))

        val eventBitmap = parseBitmap(BITMAP_SIZE, bitUtils)

        /*
         * EventDisplayData
         */
        val eventDisplay: Int
        if (eventBitmap[BITMAP_SIZE - 1]) {
            eventDisplay = bitUtils.getNextInteger(EVENT_DISPLAY_SIZE)
        }

        /*
         * EventNetworkId
         */
        val eventNetworkId: String
        if (eventBitmap[BITMAP_SIZE - 2]) {
            eventNetworkId = bitUtils.getNextHexaString(EVENT_NETWORK_ID_SIZE)
        }

        /*
         * EventCode
         */
        val eventCode: String
        if (eventBitmap[BITMAP_SIZE - 3]) {
            eventCode = bitUtils.getNextHexaString(EVENT_CODE_SIZE)
            val eventCodeBitUtils = BitUtils(BytesUtils.fromString(eventCode))
            eventCodeBitUtils.currentBitIndex = 0
            val eventTransport = IntercodeEventTransportEnum.findEnumByKey(eventCodeBitUtils.getNextInteger(4))
            val eventType = IntercodeEventTypeEnum.findEnumByKey(eventCodeBitUtils.getNextInteger(4))
            name = "${eventTransport.value} - ${eventType.value}"
        }

        /*
         * EventResult
         */
        val eventResult: Int
        if (eventBitmap[BITMAP_SIZE - 4]) {
            eventResult = bitUtils.getNextInteger(EVENT_RESULT_SIZE)
        }

        /*
         * EventServiceProvider
         */
        var provider: Int? = null
        if (eventBitmap[BITMAP_SIZE - 5]) {
            provider = bitUtils.getNextInteger(EVENT_SERVICE_PROVIDER_SIZE)
        }

        /*
         * EventNotokCounter
         */
        val eventNotOkCounter: Int
        if (eventBitmap[BITMAP_SIZE - 6]) {
            eventNotOkCounter = bitUtils.getNextInteger(EVENT_NOT_OK_COUNTER_SIZE)
        }

        /*
         * EventSerialNumber
         */
        val eventSerialNumber: Int
        if (eventBitmap[BITMAP_SIZE - 7]) {
            eventSerialNumber = bitUtils.getNextInteger(EVENT_SERIAL_NUMBER_SIZE)
        }

        /*
         * EventDestination
         */
        val eventDestination: Int
        if (eventBitmap[BITMAP_SIZE - 8]) {
            eventDestination = bitUtils.getNextInteger(EVENT_DESTINATION_SIZE)
            destination = "$eventDestination"
        }

        /*
         * EventLocationId
         */
        val eventLocationId: String?
        if (eventBitmap[BITMAP_SIZE - 9]) {
            eventLocationId = bitUtils.getNextHexaString(EVENT_LOCATION_ID_SIZE)
            val bEventCode = BytesUtils.fromString(eventLocationId)
            val hVal = bEventCode[0].toInt() shr 4 and 0x0f
            if (provider == 2 || provider == 3 && (hVal == 5 || hVal == 3)) {
                location = PlaceUtils.getIseNse(eventLocationId)
            }
        }

        return EventValidation(
            name = name,
            location = location,
            destination = destination,
            date = eventDate,
            provider = provider
        )
    }

    override fun generate(content: EventValidation): ByteArray {
        TODO("Not yet implemented")
    }

    companion object {
        const val EVENT_DATE_STAMP_SIZE = 14
        const val EVENT_TIME_STAMP_SIZE = 11
        const val BITMAP_SIZE = 28

        const val EVENT_DISPLAY_SIZE = 8
        const val EVENT_NETWORK_ID_SIZE = 24
        const val EVENT_CODE_SIZE = 8
        const val EVENT_RESULT_SIZE = 8
        const val EVENT_SERVICE_PROVIDER_SIZE = 8
        const val EVENT_NOT_OK_COUNTER_SIZE = 8
        const val EVENT_SERIAL_NUMBER_SIZE = 24
        const val EVENT_DESTINATION_SIZE = 16
        const val EVENT_LOCATION_ID_SIZE = 16
    }
}
