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

import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.parser.model.ContractStructureDto
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import org.eclipse.keyple.parser.model.type.VersionNumberEnum
import org.eclipse.keyple.parser.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Calendar


@RunWith(RobolectricTestRunner::class)
class ContractStructureParserTest {

    private val contractParser =
        ContractStructureParser()

    private val sdf: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy")

    @Test
    fun parseContract1() {
        val content = BytesUtils.fromString(DATA_CONTRACT_1)

        val contract = contractParser.parse(content)

        assertNotNull(contract)
        assertEquals(VersionNumberEnum.CURRENT_VERSION, contract.contractVersionNumber)
        assertEquals(ContractPriorityEnum.SEASON_PASS, contract.contractTariff)
        assertEquals(4031, contract.contractSaleDate)
        assertEquals(4061, contract.contractValidityEndDate)
        assertEquals(sdf.parse("14/01/2021"), contract.getContractSaleDateAsDate())
        assertEquals(sdf.parse("13/02/2021"), contract.getContractValidityEndDateAsDate())
        assertEquals(0, contract.contractSaleSam)
        assertEquals(0, contract.contractSaleCounter)
        assertEquals(0, contract.contractAuthKvc)
        assertEquals(0, contract.contractAuthenticator)
    }

    @Test
    fun generateContract1() {
        val calendar = Calendar.getInstance()

        calendar.set(2021, Calendar.JANUARY, 14, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val contractSaleDate = calendar.time

        calendar.set(Calendar.MONTH, Calendar.FEBRUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 13)
        val contractValidityEndDate = calendar.time

        val contract = ContractStructureDto(
            contractVersionNumber = VersionNumberEnum.CURRENT_VERSION,
            contractTariff = ContractPriorityEnum.SEASON_PASS,
            contractSaleDate = DateUtils.dateToDateCompact(contractSaleDate),
            contractValidityEndDate = DateUtils.dateToDateCompact(contractValidityEndDate),
            contractSaleSam = 0,
            contractSaleCounter = 0,
            contractAuthKvc = 0,
            contractAuthenticator = 0
        )


        val content = ContractStructureParser().generate(contract)

        assertEquals(DATA_CONTRACT_1, BytesUtils.bytesToString(content))
    }


    companion object {
        private const val DATA_CONTRACT_1 =
            "01 01 0F BF 0F DD 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
    }
}
