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
import org.eclipse.keyple.parser.intercode.parser.IntercodeContractFFParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class IntercodeContractFFParserTest {

    val contractParser =
        IntercodeContractFFParser()

    @Test
    fun parseIntercodeContractFF() {
        val content = BytesUtils.fromString(DATA)

        val intercodeContractFF = contractParser.parse(content)

        assertNotNull(intercodeContractFF)
        assertNull(intercodeContractFF.contractNetworkId)
        assertEquals(0, intercodeContractFF.contractProvider)
        assertEquals(3, intercodeContractFF.contractTariff)
    }

    companion object{
        const val DATA = "5A5060000031200550456921A4803D67B40FC8E3FC01B81FFFFFFFFFE0"

        const val DATA_1 = "5A 50 60 00 00 01 66 3A C0 45 81 D2 09 21 FD 81 CC 0C 54 B8 04 01 B4 00 00 00 00 00 00"
        const val DATA_2 = "5A 50 60 00 00 01 66 3A C0 45 82 4E 0B 01 FD 82 4C 0C 54 B8 04 01 B8 00 00 00 00 00 00"
        const val DATA_3 = "5A 50 60 00 00 01 66 3A C0 45 7A AD EC 91 FD 7A AC 0A 3A EC 04 01 80 00 00 00 00 00 00"
        const val DATA_4 = "5A 50 60 00 00 01 66 3A C0 45 81 5A 07 31 FD 81 58 08 BF D0 04 01 A8 00 00 00 00 00 00"
    }
}
