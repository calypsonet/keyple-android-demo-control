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
package org.eclipse.keyple.parser.keyple

import fr.devnied.bitlib.BytesUtils
import java.util.Calendar
import org.eclipse.keyple.parser.model.EventStructureDto
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import org.eclipse.keyple.parser.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventStructureParserTest {

    private val eventParser =
        EventStructureParser()

    @Test
    fun parseEvent1() {
        val content = BytesUtils.fromString(DATA_EVENT_1)

        val event = eventParser.parse(content)

        assertNotNull(event)
        assertEquals(1, event.eventVersionNumber)
        assertEquals(4031, event.eventDateStamp)
        assertEquals(840, event.eventTimeStamp)
        assertEquals("Thu Jan 14 00:00:00 CET 2021", event.getEventDateStampAsDate().toString())
        assertEquals("Fri Jan 01 14:00:00 CET 2010", event.getEventTimeStampAsDate().toString())
        assertEquals("Thu Jan 14 14:00:00 CET 2021", event.getEventDate().toString())
        assertEquals(1, event.eventLocation)
        assertEquals(1, event.eventContractUsed)
        assertEquals(ContractPriorityEnum.SEASON_PASS, event.contractPriority1)
        assertEquals(ContractPriorityEnum.FORBIDDEN, event.contractPriority2)
        assertEquals(ContractPriorityEnum.FORBIDDEN, event.contractPriority3)
        assertEquals(ContractPriorityEnum.FORBIDDEN, event.contractPriority4)
    }

    @Test
    fun generateEvent1() {
        val calendar = Calendar.getInstance()

        calendar.set(2021, Calendar.JANUARY, 14, 14, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val eventDate = calendar.time

        val event = EventStructureDto(
            eventVersionNumber = 1,
            eventDateStamp = DateUtils.dateToDateCompact(eventDate),
            eventTimeStamp = DateUtils.dateToTimeCompact(eventDate),
            eventLocation = 1,
            eventContractUsed = 1,
            contractPriority1 = ContractPriorityEnum.SEASON_PASS,
            contractPriority2 = ContractPriorityEnum.FORBIDDEN,
            contractPriority3 = ContractPriorityEnum.FORBIDDEN,
            contractPriority4 = ContractPriorityEnum.FORBIDDEN
        )

        val content = EventStructureParser().generate(event)

        assertEquals(DATA_EVENT_1, BytesUtils.bytesToString(content))
    }

    companion object {
        private const val DATA_EVENT_1 =
            "01 0F BF 03 48 00 00 00 01 01 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
    }
}
