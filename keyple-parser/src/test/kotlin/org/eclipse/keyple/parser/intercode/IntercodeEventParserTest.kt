/*
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

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
package org.eclipse.keyple.parser.intercode

import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.parser.intercode.parser.IntercodeEventParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntercodeEventParserTest {

    val eventParser =
        IntercodeEventParser()

    @Test
    fun parseEvent1() {
        val content = BytesUtils.fromString(DATA_EVENT_1)
        val event = eventParser.parse(content)

        assertNotNull(event)
        assertEquals("Métro - validation en entrée", event.name)
        assertEquals("Mon Nov 30 18:31:00 CET 2020", event.date.toString())
        assertEquals(3, event.provider)
        assertNull(event.location)
        assertNull(event.destination)
    }

    @Test
    fun parseEvent2() {
        val content = BytesUtils.fromString(DATA_EVENT_2)
        val event = eventParser.parse(content)

        assertNotNull(event)
        assertEquals("Tramway - validation en entrée", event.name)
        assertEquals("Thu Dec 03 18:49:00 CET 2020", event.date.toString())
        assertEquals(3, event.provider)
        assertNull(event.location)
        assertNull(event.destination)
    }

    @Test
    fun parseEvent3() {
        val content = BytesUtils.fromString(DATA_EVENT_3)
        val event = eventParser.parse(content)

        assertNotNull(event)
        assertEquals("Train - validation en sortie", event.name)
        assertEquals("Sat Nov 09 17:17:00 CET 2019", event.date.toString())
        assertEquals(3, event.provider)
        assertNull(event.location)
        assertNull(event.destination)
    }

    companion object{
        private const val DATA_EVENT_1 = "88 7A 2B 90 00 68 A1 88 18 96 86 88 08 00 80 40 00 00 00 00 00 00 00 00 00 00 00 00 00"
        private const val DATA_EVENT_2 = "88 86 34 90 03 68 A2 08 1C 00 80 36 98 00 68 00 10 0A 08 40 00 00 00 00 00 00 00 00 00"
        private const val DATA_EVENT_3 = "82 6E 06 90 00 68 A2 90 19 13 09 10 80 00 88 80 00 00 00 00 00 00 00 00 00 00 00 00 00"
        private const val DATA_EVENT_4 = "82 6D F2 10 00 68 A2 B0 19 00 8A 10 48 00 80 80 00 00 00 00 00 00 00 00 00 00 00 00 00"
    }
}
