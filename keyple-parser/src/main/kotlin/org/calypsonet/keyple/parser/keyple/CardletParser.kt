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
package org.calypsonet.keyple.parser.keyple

import org.calypsonet.keyple.parser.IParser
import org.calypsonet.keyple.parser.dto.CardletInputDto
import org.calypsonet.keyple.parser.model.CardletDto
import org.calypsonet.keyple.parser.model.ContractStructureDto
import org.calypsonet.keyple.parser.model.EventStructureDto

class CardletParser : IParser<CardletDto?> {

    fun parseCardlet(cardletDto: CardletInputDto): CardletDto? {

        /*
         * Parse environment
         */
        val environment = EnvironmentHolderStructureParser()
            .parse(cardletDto.envData)

        /*
         * Parse contracts
         */
        val contracts = mutableListOf<ContractStructureDto>()
        cardletDto.contractData.forEach {
            contracts.add(ContractStructureParser().parse(it))
        }

        /*
         * Parse events
         */
        val events = mutableListOf<EventStructureDto>()
        cardletDto.eventData.forEach {
            events.add(EventStructureParser().parse(it))
        }

        /*
         * Parse counter
         */
        val counter = CounterStructureParser().parse(cardletDto.counterData)

        return CardletDto(
            environmentHolderStructureDto = environment,
            contractStructureDtos = contracts,
            eventStructureDtos = events,
            counterStructureDtos = mutableListOf(counter)
        )
    }

    override fun generate(content: CardletDto?): ByteArray {
        TODO("Not yet implemented")
    }

    override fun parse(content: ByteArray): CardletDto? = null
}
