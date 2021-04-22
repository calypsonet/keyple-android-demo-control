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

package org.eclipse.keyple.parser.keyple

import fr.devnied.bitlib.BitUtils
import org.eclipse.keyple.parser.IParser
import org.eclipse.keyple.parser.model.ContractStructureDto
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import org.eclipse.keyple.parser.model.type.VersionNumberEnum
import java.math.BigInteger


class ContractStructureParser :
    IParser<ContractStructureDto> {

    override fun parse(content: ByteArray): ContractStructureDto {

        val bitUtils = BitUtils(content)
        bitUtils.currentBitIndex = 0

        /*
         * contractVersionNumber
         */
        val tempCVN = bitUtils.getNextInteger(CONTRACT_VERSION_NUMBER_SIZE)
        val contractVersionNumber = VersionNumberEnum.findEnumByKey(tempCVN)

        /*
         * contractTariff
         */
        val tempTariff = bitUtils.getNextInteger(CONTRACT_TARIFF_SIZE)
        val contractTariff = ContractPriorityEnum.findEnumByKey(tempTariff)

        /*
         * contractSaleDate
         */
        val contractSaleDate = bitUtils.getNextInteger(CONTRACT_SALE_DATE_SIZE)

        /*
         * contractValidityEndDate
         */
        val contractValidityEndDate = bitUtils.getNextInteger(CONTRACT_VALIDITY_END_DATE_SIZE)

        /*
         * contractSaleSam
         */
        val contractSaleSam = bitUtils.getNextInteger(CONTRACT_SALE_SAM_SIZE)

        /*
         * contractSaleCounter
         */
        val contractSaleCounter = bitUtils.getNextInteger(CONTRACT_SALE_COUNTER_SIZE)

        /*
         * contractAuthKvc
         */
        val contractAuthKvc = bitUtils.getNextInteger(CONTRACT_AUTH_KVC_SIZE)

        /*
         * contractAuthenticator
         */
        val contractAuthenticator = bitUtils.getNextInteger(CONTRACT_AUTHENTICATOR_SIZE)


        return ContractStructureDto(
            contractVersionNumber = contractVersionNumber,
            contractValidityEndDate = contractValidityEndDate,
            contractSaleDate = contractSaleDate,
            contractTariff = contractTariff,
            contractSaleSam = contractSaleSam,
            contractSaleCounter = contractSaleCounter,
            contractAuthKvc = contractAuthKvc,
            contractAuthenticator = contractAuthenticator
        )
    }

    override fun generate(content: ContractStructureDto): ByteArray {

        val bitUtils = BitUtils(CONTRACT_SIZE)
        bitUtils.currentBitIndex = 0

        /*
         * contractVersionNumber
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(content.contractVersionNumber.key.toLong()).toByteArray(),
            CONTRACT_VERSION_NUMBER_SIZE
        )

        /*
         * contractTariff
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(content.contractTariff.key.toLong()).toByteArray(),
            CONTRACT_TARIFF_SIZE
        )

        /*
         * contractSaleDate
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(content.contractSaleDate.toLong()).toByteArray(),
            CONTRACT_SALE_DATE_SIZE
        )

        /*
         * contractValidityEndDate
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(content.contractValidityEndDate.toLong()).toByteArray(),
            CONTRACT_VALIDITY_END_DATE_SIZE
        )

        /*
         * contractSaleSam
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(content.contractSaleSam?.toLong() ?: 0).toByteArray(),
            CONTRACT_SALE_SAM_SIZE
        )

        /*
         * contractSaleCounter
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(content.contractSaleCounter?.toLong() ?: 0).toByteArray(),
            CONTRACT_SALE_COUNTER_SIZE
        )

        /*
         * contractAuthKvc
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(content.contractAuthKvc?.toLong() ?: 0).toByteArray(),
            CONTRACT_AUTH_KVC_SIZE
        )

        /*
         * contractAuthenticator
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(content.contractAuthenticator?.toLong() ?: 0).toByteArray(),
            CONTRACT_AUTHENTICATOR_SIZE
        )

        /*
         * Padding
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(0).toByteArray(),
            CONTRACT_PADDING
        )

        return bitUtils.data
    }

    companion object {
        const val CONTRACT_SIZE = 232

        const val CONTRACT_VERSION_NUMBER_SIZE = 8
        const val CONTRACT_TARIFF_SIZE = 8
        const val CONTRACT_SALE_DATE_SIZE = 16
        const val CONTRACT_VALIDITY_END_DATE_SIZE = 16
        const val CONTRACT_SALE_SAM_SIZE = 32
        const val CONTRACT_SALE_COUNTER_SIZE = 24
        const val CONTRACT_AUTH_KVC_SIZE = 8
        const val CONTRACT_AUTHENTICATOR_SIZE = 24

        const val CONTRACT_PADDING = 96
    }
}