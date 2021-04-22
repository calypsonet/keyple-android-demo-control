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

package org.eclipse.keyple.parser.intercode.parser

import fr.devnied.bitlib.BitUtils
import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.parser.IParser
import org.eclipse.keyple.parser.intercode.model.AbstractIntercodeContract
import org.eclipse.keyple.parser.intercode.model.IntercodeContractFF


class IntercodeContractFFParser:
    IParser<AbstractIntercodeContract> {

    override fun parse(content: ByteArray): IntercodeContractFF {

        val bitUtils = BitUtils(content)
        bitUtils.currentBitIndex = 0

        val bitmap = parseBitmap(BITMAP_SIZE, bitUtils)

        var contractNetworkId: String? = null
        if(bitmap[BITMAP_SIZE -1]){
            contractNetworkId = bitUtils.getNextHexaString(24)
        }

        var contractProvider: Int? = null
        if(bitmap[BITMAP_SIZE -2]){
            contractProvider = bitUtils.getNextInteger(8)
        }

        var contractTariff: Int? = null
        if(bitmap[BITMAP_SIZE -3]){
            contractTariff = bitUtils.getNextInteger(16)
        }


        return IntercodeContractFF(
            contract = bitmap,
            contractNetworkId = contractNetworkId,
            contractProvider = contractProvider,
            contractTariff = contractTariff
        )
    }

    override fun generate(content: AbstractIntercodeContract): ByteArray {
        TODO("Not yet implemented")
    }

    companion object {
        const val BITMAP_SIZE = 20
    }
}