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
import org.eclipse.keyple.parser.IParser
import org.eclipse.keyple.parser.intercode.model.IntercodeBestContract
import org.eclipse.keyple.parser.intercode.model.IntercodeBestContractList


class IntercodeBestContractListParser :
    IParser<IntercodeBestContractList> {

    override fun parse(content: ByteArray): IntercodeBestContractList {

        val bitUtils = BitUtils(content)
        bitUtils.currentBitIndex = 0

        val contractNb = bitUtils.getNextInteger(BEST_CONTRACT_NB_SIZE)

        val contracts = mutableListOf<IntercodeBestContract>()

        for (i in 0 until contractNb) {
            val bitmap = parseBitmap(
                size = BEST_CONTRACT_BITMAP_SIZE,
                bitUtils = bitUtils
            )
            var networkId: Int? = null
            if(bitmap[BEST_CONTRACT_BITMAP_SIZE -1] == true){
                /*
                 * Handle BestContractNetworkId
                 */
                networkId = bitUtils.getNextInteger(BEST_CONTRACT_NETWORK_ID_SIZE)
            }
            var tariff = 0
            if(bitmap[BEST_CONTRACT_BITMAP_SIZE -2] == true){
                /*
                 * Handle BestContractTariff
                 */
                tariff = bitUtils.getNextInteger(BEST_CONTRACT_TARIFF_SIZE)

            }
            var pointer = 0
            if(bitmap[BEST_CONTRACT_BITMAP_SIZE -3] == true){
                /*
                 * Handle BestContractPointer
                 */
                pointer = bitUtils.getNextInteger(BEST_CONTRACT_POINTER_SIZE)
            }

            contracts.add(
                IntercodeBestContract(
                    bitmap = bitmap,
                    bestContractPointer = pointer,
                    bestContratNetworkId = networkId,
                    bestContratTariff = tariff
                )
            )
        }

        return IntercodeBestContractList(
            bestContracts = contracts
        )
    }

    override fun generate(content: IntercodeBestContractList): ByteArray {
        TODO("Not yet implemented")
    }

    companion object {
        const val BEST_CONTRACT_NB_SIZE = 4
        const val BEST_CONTRACT_BITMAP_SIZE = 3
        const val BEST_CONTRACT_NETWORK_ID_SIZE = 3 * Byte.SIZE_BITS
        const val BEST_CONTRACT_TARIFF_SIZE = 16
        const val BEST_CONTRACT_POINTER_SIZE = 5
    }
}