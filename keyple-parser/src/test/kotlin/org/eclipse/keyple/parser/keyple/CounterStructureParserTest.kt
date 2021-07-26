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
import org.eclipse.keyple.parser.model.CounterStructureDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CounterStructureParserTest {

    private val counterParser =
        CounterStructureParser()

    @Test
    fun parseContract1() {
        val content = BytesUtils.fromString(DATA_COUNTER_1)

        val counter = counterParser.parse(content)

        assertNotNull(counter)
        assertEquals(10, counter.counterValue)
    }

    @Test
    fun generateContract1() {
        val counter = CounterStructureDto(
            counterValue = 10
        )

        val content = CounterStructureParser().generate(counter)

        assertEquals(DATA_COUNTER_1, BytesUtils.bytesToString(content))
    }

    companion object {
        private const val DATA_COUNTER_1 =
            "00 00 00 0A"
    }
}
