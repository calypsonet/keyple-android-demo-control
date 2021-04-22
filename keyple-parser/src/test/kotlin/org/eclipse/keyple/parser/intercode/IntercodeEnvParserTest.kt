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
import org.eclipse.keyple.parser.intercode.parser.IntercodeEnvParser
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat


@RunWith(RobolectricTestRunner::class)
class IntercodeEnvParserTest {

    private val envParser =
        IntercodeEnvParser()

    private val sdf: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy")

    @Test
    fun parseEnv1() {
        val content = BytesUtils.fromString(DATA_ENV_1)
        val environment = envParser.parse(content)

        assertNotNull(environment)
        assertEquals(9, environment.envApplicationVersionNumber)
        assertEquals(1, environment.getIntercodeVersion())
        assertEquals(1, environment.getApplicationVersion())
        assertEquals("250901", environment.envNetworkId)
        assertEquals(0, environment.envApplicationIssuerId)
        assertEquals(sdf.parse("31/01/2030"), environment.envApplicationValidityEndDate)
        assertEquals("2305", environment.envAuthenticator)
        assertNull(environment.envPayMethod)
        assertNull(environment.envSelectList)
    }

    @Test
    fun parseEnv2() {
        val content = BytesUtils.fromString(DATA_ENV_2)
        val environment = envParser.parse(content)

        assertNotNull(environment)
        assertEquals(9, environment.envApplicationVersionNumber)
        assertEquals(1, environment.getIntercodeVersion())
        assertEquals(1, environment.getApplicationVersion())
        assertEquals("250901", environment.envNetworkId)
        assertEquals(0, environment.envApplicationIssuerId)
        assertEquals(sdf.parse("18/07/2027"), environment.envApplicationValidityEndDate)
        assertEquals("8280", environment.envAuthenticator)
        assertNull(environment.envPayMethod)
        assertNull(environment.envSelectList)
    }

    @Test
    fun parseEnv3() {
        val content = BytesUtils.fromString(DATA_ENV_3)
        val environment = envParser.parse(content)

        assertArrayEquals(
            booleanArrayOf(false, false, true, false, true, true, true),
            environment.generalBitmap
        )
        assertEquals(9, environment.envApplicationVersionNumber)
        assertEquals(1, environment.getIntercodeVersion())
        assertEquals(1, environment.getApplicationVersion())
        assertEquals("250901", environment.envNetworkId)
        assertEquals(PROVIDER_SNCF, environment.envApplicationIssuerId)
        assertEquals(sdf.parse("05/03/2025"), environment.envApplicationValidityEndDate)
        assertEquals("C9D2", environment.envAuthenticator)
        assertNull(environment.envPayMethod)
        assertNull(environment.envSelectList)
    }

    @Test
    fun parseEnv4() {
        val content = BytesUtils.fromString(DATA_ENV_4)
        val environment = envParser.parse(content)

        assertArrayEquals(
            booleanArrayOf(false, false, true, false, true, true, true),
            environment.generalBitmap
        )
        assertEquals(9, environment.envApplicationVersionNumber)
        assertEquals(1, environment.getIntercodeVersion())
        assertEquals(1, environment.getApplicationVersion())
        assertEquals("250901", environment.envNetworkId)
        assertEquals(PROVIDER_SNCF, environment.envApplicationIssuerId)
        assertEquals(sdf.parse("05/03/2025"), environment.envApplicationValidityEndDate)
        assertEquals("C9D2", environment.envAuthenticator)
        assertNull(environment.envPayMethod)
        assertNull(environment.envSelectList)
    }

    companion object {
        /** Provider national SNCF  */
        private const val PROVIDER_SNCF = 0x02

        private const val DATA_ENV_1 =
            "24 B9 28 48 08 05 E6 64 60 B0 00 12 C1 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
        private const val DATA_ENV_2 =
            "24 B9 28 48 08 05 72 70 50 10 00 12 40 80 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
        private const val DATA_ENV_3 =
            "24 B9 28 48 08 15 06 59 3A 50 00 12 04 80 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
        private const val DATA_ENV_4 =
            "24 B9 28 48 08 15 06 59 3A 40 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
    }

}
