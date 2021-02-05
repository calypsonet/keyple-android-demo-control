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
import fr.devnied.bitlib.BytesUtils
import org.eclipse.keyple.parser.IParser
import org.eclipse.keyple.parser.IParser.Companion.DATE_01_01_2010
import org.eclipse.keyple.parser.model.EnvironmentHolderStructureDto
import java.math.BigInteger
import java.util.Calendar


class EnvironmentHolderStructureParser :
    IParser<EnvironmentHolderStructureDto> {

    override fun parse(content: ByteArray): EnvironmentHolderStructureDto {

        val bitUtils = BitUtils(content)
        bitUtils.currentBitIndex = 0

        /*
         * envVersionNumber
         */
        val envVersionNumber = bitUtils.getNextInteger(ENV_EVN_SIZE)

        /*
         * envApplicationNumber
         */
        val envApplicationNumber = bitUtils.getNextInteger(ENV_AVN_SIZE)

        /*
         * envIssuingDate
         */
        val envIssuingDate = bitUtils.getNextInteger(ENV_ISSUING_DATE_SIZE)

        /*
         * envEndDate
         */
        val envEndDate = bitUtils.getNextInteger(ENV_END_DATE_SIZE)

        /*
         * holderCompany
         */
        val holderCompany = bitUtils.getNextInteger(ENV_HOLDER_COMPANY_SIZE)

        /*
         * holderIdNumber
         */
        val holderIdNumber = bitUtils.getNextInteger(ENV_HOLDER_ID_NUMBER_SIZE)

        return EnvironmentHolderStructureDto(
            envVersionNumber = envVersionNumber,
            envApplicationNumber = envApplicationNumber,
            envEndDate = envEndDate,
            envIssuingDate = envIssuingDate,
            holderCompany = holderCompany,
            holderIdNumber = holderIdNumber
        )
    }

    override fun generate(environment: EnvironmentHolderStructureDto): ByteArray {

        val bitUtils = BitUtils(ENV_SIZE)
        /*
         * envVersionNumber
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(environment.envVersionNumber.toLong()).toByteArray(), ENV_EVN_SIZE
        )

        /*
         * envApplicationNumber
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(environment.envApplicationNumber.toLong()).toByteArray(),
            ENV_AVN_SIZE
        )

        /*
         * envIssuingDate
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(environment.envIssuingDate.toLong()).toByteArray(),
            ENV_ISSUING_DATE_SIZE
        )

        /*
         * envEndDate
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(environment.envEndDate.toLong()).toByteArray(),
            ENV_END_DATE_SIZE
        )

        /*
         * holderCompany
         */
        val holderCompany = environment.holderCompany ?: 0
        bitUtils.setNextByte(
            BigInteger.valueOf(holderCompany.toLong()).toByteArray(),
            ENV_HOLDER_COMPANY_SIZE
        )

        /*
         * holderIdNumber
         */
        val holderIdNumber = environment.holderIdNumber ?: 0
        bitUtils.setNextByte(
            BigInteger.valueOf(holderIdNumber.toLong()).toByteArray(),
            ENV_HOLDER_ID_NUMBER_SIZE
        )

        /*
         * padding
         */
        bitUtils.setNextByte(BigInteger.valueOf(0).toByteArray(), ENV_PADDING)

        return bitUtils.data
    }

    companion object {
        const val ENV_SIZE = 232

        const val ENV_EVN_SIZE = 8
        const val ENV_AVN_SIZE = 32
        const val ENV_ISSUING_DATE_SIZE = 16
        const val ENV_END_DATE_SIZE = 16
        const val ENV_HOLDER_COMPANY_SIZE = 8
        const val ENV_HOLDER_ID_NUMBER_SIZE = 32
        const val ENV_PADDING = 120
    }
}