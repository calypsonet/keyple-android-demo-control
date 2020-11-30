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

import org.eclipse.keyple.parser.IParser
import org.eclipse.keyple.parser.intercode.model.AbstractIntercodeContract
import org.eclipse.keyple.parser.intercode.model.EventValidation
import org.eclipse.keyple.parser.intercode.model.IntercodeCardlet
import org.eclipse.keyple.parser.intercode.model.IntercodeCardletDto


class IntercodeCardletParser:
    IParser<IntercodeCardlet?> {

    fun parseCardlet(cardletDto: IntercodeCardletDto): IntercodeCardlet?{

        /*
         * Parse environment
         */
        val environment = IntercodeEnvParser()
            .parse(cardletDto.envData)

        /*
         * Parse contracts
         */
        val bestContractList = IntercodeBestContractListParser()
            .parse(cardletDto.bestContractListData)

        /*
         * Parse contracts
         */
        val contracts = mutableListOf<AbstractIntercodeContract>()
        for (i in bestContractList.bestContracts.indices) {
            var contract: ByteArray
            if(bestContractList.bestContracts[i].getBestContratType() == 0xFF){
                contract = cardletDto.contractData[i]
//                contracts.add(IntercodeContractFFParser().parse(contract))
//                contracts.add(IntercodePublicTransportContractParser().parse(contract))

                val intercodeContractFFParser = IntercodeContractFFParser()
                    .parse(contract)
                val intercodePublicTransportContractParser = IntercodePublicTransportContractParser()
                    .parse(contract)

                contracts.add(intercodeContractFFParser)
            }
            else{
                contract = cardletDto.contractData[i]
                contracts.add(
                    IntercodePublicTransportContractParser()
                        .parse(contract))
            }
        }

        /*
         * Parse events
         */
        val events = mutableListOf<EventValidation>()
        cardletDto.eventData.forEach {
            events.add(
                IntercodeEventParser()
                    .parse(it))
        }

        return IntercodeCardlet(
            environment = environment,
            bestContractList = bestContractList,
            contracts = contracts,
            events = events
        )

        return null
    }

    override fun parse(content: ByteArray): IntercodeCardlet? = null

    override fun generate(content: IntercodeCardlet?): ByteArray {
        TODO("Not yet implemented")
    }

    companion object {
    }
}