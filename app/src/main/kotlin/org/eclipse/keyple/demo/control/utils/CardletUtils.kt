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

package org.eclipse.keyple.demo.control.utils

import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.demo.control.ticketing.CalypsoInfo
import org.eclipse.keyple.parser.dto.CardletInputDto
import org.eclipse.keyple.parser.keyple.ContractStructureParser
import org.eclipse.keyple.parser.keyple.CounterStructureParser
import org.eclipse.keyple.parser.keyple.EventStructureParser
import org.eclipse.keyple.parser.model.ContractStructureDto
import org.eclipse.keyple.parser.model.CounterStructureDto
import org.eclipse.keyple.parser.model.EventStructureDto
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import org.eclipse.keyple.parser.model.type.VersionNumberEnum
import org.eclipse.keyple.parser.utils.DateUtils
import java.nio.ByteBuffer
import java.util.Calendar
import java.util.Date

/**
 *  @author youssefamrani
 */

object CardletUtils {

    fun getEmptyFile(): ByteArray {
        return ByteBuffer.allocate(29).array()
    }

    fun getContractSeasonPass(): ByteArray {
        return getContract(ContractPriorityEnum.SEASON_PASS)
    }

    fun getContractMultiTrip(): ByteArray {
        return getContract(ContractPriorityEnum.MULTI_TRIP)
    }

    private fun getContract(contractTariff: ContractPriorityEnum): ByteArray {

        val calendar = Calendar.getInstance()

        calendar.set(2021, Calendar.JANUARY, 14, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val contractSaleDate = calendar.time

        calendar.set(Calendar.MONTH, Calendar.JUNE)
        calendar.set(Calendar.DAY_OF_MONTH, 13)
        val contractValidityEndDate = calendar.time

        val contract = ContractStructureDto(
            contractVersionNumber = VersionNumberEnum.CURRENT_VERSION,
            contractTariff = contractTariff,
            contractSaleDate = DateUtils.dateToDateCompact(contractSaleDate),
            contractValidityEndDate = DateUtils.dateToDateCompact(contractValidityEndDate),
            contractSaleSam = 0,
            contractSaleCounter = 0,
            contractAuthKvc = 0,
            contractAuthenticator = 0
        )

        return ContractStructureParser().generate(contract)
    }


    fun getEventSeasonPass(): ByteArray {
        return getEvent(ContractPriorityEnum.SEASON_PASS)
    }

    fun getEventMultiTrip(): ByteArray {
        return getEvent(ContractPriorityEnum.MULTI_TRIP)
    }

    private fun getEvent(contractPriority1: ContractPriorityEnum): ByteArray {

        val calendar = Calendar.getInstance()

        calendar.set(2021, Calendar.JANUARY, 14, 14, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val eventDate = calendar.time

        val event = EventStructureDto(
            eventVersionNumber = 1,
            eventDateStamp = DateUtils.dateToDateCompact(eventDate),
            eventTimeStamp = DateUtils.dateToTimeCompact(eventDate),
            eventLocation = 1,
            eventContractUsed = 1,
            contractPriority1 = contractPriority1,
            contractPriority2 = ContractPriorityEnum.FORBIDDEN,
            contractPriority3 = ContractPriorityEnum.FORBIDDEN,
            contractPriority4 = ContractPriorityEnum.FORBIDDEN
        )

        return EventStructureParser().generate(event)
    }

    fun getEvent(eventDate: Date): ByteArray {
        val event = EventStructureDto(
            eventVersionNumber = 1,
            eventDateStamp = DateUtils.dateToDateCompact(eventDate),
            eventTimeStamp = DateUtils.dateToTimeCompact(eventDate),
            eventLocation = 1,
            eventContractUsed = 1,
            contractPriority1 = ContractPriorityEnum.SEASON_PASS,
            contractPriority2 = ContractPriorityEnum.FORBIDDEN,
            contractPriority3 = ContractPriorityEnum.FORBIDDEN,
            contractPriority4 = ContractPriorityEnum.FORBIDDEN
        )

        return EventStructureParser().generate(event)
    }

    fun getCounter(counterValue: Int): ByteArray {
        val counter = CounterStructureDto(
            counterValue = counterValue
        )

        return CounterStructureParser().generate(counter)
    }

    fun getEnvironment() = BytesUtils.fromString(CalypsoInfo.DATA_ENV)

    fun getSeasonPassCardlet(): CardletInputDto {
        return CardletInputDto(
            envData = getEnvironment(),
            contractData = mutableListOf(
                getContractSeasonPass(),
                getEmptyFile(),
                getEmptyFile()
            ),
            eventData = mutableListOf(getEventSeasonPass()),
            counterData = getEmptyFile()
        )
    }

    fun getMultiTripCardlet(): CardletInputDto {
        return CardletInputDto(
            envData = getEnvironment(),
            contractData = mutableListOf(
                getContractMultiTrip(),
                getEmptyFile(),
                getEmptyFile()
            ),
            eventData = mutableListOf(getEventMultiTrip()),
            counterData = getCounter(10)
        )
    }
}