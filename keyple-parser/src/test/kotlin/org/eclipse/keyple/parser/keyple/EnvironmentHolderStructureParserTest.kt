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
package org.eclipse.keyple.parser.keyple

import fr.devnied.bitlib.BitUtils
import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.parser.model.EnvironmentHolderStructureDto
import org.eclipse.keyple.parser.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale


@RunWith(RobolectricTestRunner::class)
class EnvironmentHolderStructureParserTest {

    private val envParser =
        EnvironmentHolderStructureParser()

    private val sdf: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy")

    @Test
    fun parseEnv1() {
        val content = BytesUtils.fromString(DATA_ENV_1)

        val environment = envParser.parse(content)

        assertNotNull(environment)
        assertEquals(9, environment.envVersionNumber)
        assertEquals(1, environment.envApplicationNumber)
        assertEquals(4091, environment.envIssuingDate)
        assertEquals(7314, environment.envEndDate)
        assertEquals(sdf.parse("15/03/2021"), environment.getEnvIssuingDateAsDate())
        assertEquals(sdf.parse("10/01/2030"), environment.getEnvEndDateAsDate())
        assertEquals(7, environment.holderCompany)
        assertEquals(8, environment.holderIdNumber)
    }

    @Test
    fun generateEnv1() {
        val calendar = Calendar.getInstance()

        calendar.set(2021, Calendar.MARCH, 15, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val envIssuingDate = calendar.time

        calendar.set(2030, Calendar.JANUARY, 10, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val envEndDate = calendar.time

        val environment = EnvironmentHolderStructureDto(
            envVersionNumber = 9,
            envApplicationNumber = 1,
            envIssuingDate = DateUtils.dateToDateCompact(envIssuingDate),
            envEndDate = DateUtils.dateToDateCompact(envEndDate),
            holderCompany = 7,
            holderIdNumber = 8
        )

        val content = EnvironmentHolderStructureParser().generate(environment)

        assertEquals(DATA_ENV_1, BytesUtils.bytesToString(content))
    }

    @Test
    fun parseEnv2() {
        val content = BytesUtils.fromString(DATA_ENV_2)

        val environment = envParser.parse(content)

        assertNotNull(environment)
        assertEquals(1, environment.envApplicationNumber)
        assertEquals(1, environment.envVersionNumber)
        assertEquals(4031, environment.envIssuingDate)
        assertEquals(6222, environment.envEndDate)
        assertEquals(sdf.parse("14/01/2021"), environment.getEnvIssuingDateAsDate())
        assertEquals(sdf.parse("14/01/2027"), environment.getEnvEndDateAsDate())
        assertEquals(0, environment.holderCompany)
        assertEquals(0, environment.holderIdNumber)
    }

    @Test
    fun generateEnv2() {
        val calendar = Calendar.getInstance()

        calendar.set(2021, Calendar.JANUARY, 14, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val envIssuingDate = calendar.time

        calendar.set(2027, Calendar.JANUARY, 14, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val envEndDate = calendar.time

        val environment = EnvironmentHolderStructureDto(
            envVersionNumber = 1,
            envApplicationNumber = 1,
            envIssuingDate = DateUtils.dateToDateCompact(envIssuingDate),
            envEndDate = DateUtils.dateToDateCompact(envEndDate),
            holderIdNumber = 0,
            holderCompany = 0
        )

        assertNotNull(environment)
        assertEquals(1, environment.envApplicationNumber)
        assertEquals(1, environment.envVersionNumber)
        assertEquals(4031, environment.envIssuingDate)
        assertEquals(6222, environment.envEndDate)
        assertEquals(sdf.parse("14/01/2021"), environment.getEnvIssuingDateAsDate())
        assertEquals(sdf.parse("14/01/2027"), environment.getEnvEndDateAsDate())
        assertEquals(0, environment.holderCompany)
        assertEquals(0, environment.holderIdNumber)

        val content = EnvironmentHolderStructureParser().generate(environment)

        assertEquals(DATA_ENV_2, BytesUtils.bytesToString(content))
    }


    companion object {
        private const val DATA_ENV_1 =
            "09 00 00 00 01 0F FB 1C 92 07 00 00 00 08 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"

        private const val DATA_ENV_2 =
            "01 00 00 00 01 0F BF 18 4E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"

        const val ENV_PADDING = 120
    }

}
