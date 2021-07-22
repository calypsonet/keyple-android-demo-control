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
package org.eclipse.keyple.parser.intercode

import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.parser.intercode.parser.IntercodePublicTransportContractParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntercodePublicTransportContractParserTest {

    val contractParser =
        IntercodePublicTransportContractParser()

    @Test
    fun parsePublicTransportContract() {
        val content = BytesUtils.fromString(DATA)

        val intercodePublicTransportContract = contractParser.parse(content)

        assertNotNull(intercodePublicTransportContract)
        assertEquals(2, intercodePublicTransportContract.contractProvider)
        assertEquals(828, intercodePublicTransportContract.contractTariffval)
        assertEquals("Wed Oct 17 00:00:00 CEST 2012", intercodePublicTransportContract.contractValidityStartDate.toString())
        assertEquals("Fri Oct 16 00:00:00 CEST 2015", intercodePublicTransportContract.contractValidityEndDate.toString())
        assertEquals(0, intercodePublicTransportContract.contractStatus)
    }

    companion object {
        const val DATA = "E6 04 06 79 AD 10 D6 70 01 B4 68 04 0F FF F8 04 02 D1 00 10 20 00 00 00 00 00 10 00 00"
    }
}
