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

package org.eclipse.keyple.parser.intercode

import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.parser.intercode.parser.IntercodeBestContractListParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntercodeBestContractListParserTest {

    private val parser =
        IntercodeBestContractListParser()

    @Test
    fun parseBestContract_00() {
        val content = BytesUtils.fromString(DATA_00)
        val bestContractList = parser.parse(content)

        assertNotNull(bestContractList)
        assertNotNull(bestContractList.bestContracts)
        assertEquals(4, bestContractList.bestContracts.size)

        val contract1 = bestContractList.bestContracts[0]
        assertEquals(4, contract1.bestContractPointer)
        assertEquals(4089, contract1.bestContratTariff)

        val contract2 = bestContractList.bestContracts[1]
        assertEquals(2, contract2.bestContractPointer)
        assertEquals(4095, contract2.bestContratTariff)

        val contract3 = bestContractList.bestContracts[2]
        assertEquals(3, contract3.bestContractPointer)
        assertEquals(4095, contract3.bestContratTariff)

        val contract4 = bestContractList.bestContracts[3]
        assertEquals(1, contract4.bestContractPointer)
        assertEquals(4089, contract4.bestContratTariff)
    }

    @Test
    fun parseBestContract_01() {
        val content = BytesUtils.fromString(DATA_01)
        val bestContractList = parser.parse(content)

        assertNotNull(bestContractList)
        assertNotNull(bestContractList.bestContracts)
        assertEquals(4, bestContractList.bestContracts.size)

        val contract1 = bestContractList.bestContracts[0]
        assertEquals(4, contract1.bestContractPointer)
        assertEquals(8183, contract1.bestContratTariff)

        val contract2 = bestContractList.bestContracts[1]
        assertEquals(2, contract2.bestContractPointer)
        assertEquals(8183, contract2.bestContratTariff)

        val contract3 = bestContractList.bestContracts[2]
        assertEquals(3, contract3.bestContractPointer)
        assertEquals(8183, contract3.bestContratTariff)

        val contract4 = bestContractList.bestContracts[3]
        assertEquals(1, contract4.bestContractPointer)
        assertEquals(4089, contract4.bestContratTariff)
    }

    companion object {
        private const val DATA_00 = "4C1FF24C1FFE2C1FFE3C1FF21000000000000000000000000000000000"
        private const val DATA_01 = "4C3FEE4C3FEE2C3FEE3C1FF21000000000000000000000000000000000"
        private const val DATA_02 = "4C 1F F2 1C 1F F2 2C 1F FE 4C 1F FE 30 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
    }
}
