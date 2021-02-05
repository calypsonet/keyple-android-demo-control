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
import org.eclipse.keyple.parser.IParser.Companion.DATE_01_01_1997
import org.eclipse.keyple.parser.intercode.model.AbstractIntercodeContract
import org.eclipse.keyple.parser.intercode.model.IntercodePublicTransportContract
import java.util.Date


class IntercodePublicTransportContractParser:
    IParser<AbstractIntercodeContract> {

    override fun parse(content: ByteArray): IntercodePublicTransportContract {

        val bitUtils = BitUtils(content)
        bitUtils.currentBitIndex = 0

        val bitmap = parseBitmap(BITMAP_SIZE, bitUtils)

        var contractProvider: Int? = null
        if (bitmap[BITMAP_SIZE - 1]) {
            contractProvider = bitUtils.getNextInteger(8)
        }

        var contractTariffval: Int? = null
        if (bitmap[BITMAP_SIZE - 2]) {
            contractTariffval = bitUtils.getNextInteger(16)
        }

        var contractSerialNumber: Int? = null
        if (bitmap[BITMAP_SIZE - 3]) {
            contractSerialNumber = bitUtils.getNextInteger(32)
        }

        var contractPassengerClass: Int? = null
        if (bitmap[BITMAP_SIZE - 4]) {
            contractPassengerClass = bitUtils.getNextInteger(8)
        }

        var contractValidityInfo: ByteArray? = null
        var contractValidityStartDate: Date? = null
        var contractValidityEndDate: Date? = null
        if (bitmap[BITMAP_SIZE - 5]) {
            contractValidityInfo = bitUtils.getNextByte(2)

            val contractValidityInfoBitUtils = BitUtils(contractValidityInfo)
            contractValidityInfoBitUtils.currentBitIndex = 0
            val contractValidityInfoBitmap = parseBitmap(2, contractValidityInfoBitUtils)


            if (contractValidityInfoBitmap[1]) {
                contractValidityStartDate = parseDate(bitUtils.getNextInteger(14), DATE_01_01_1997)
            }

            if (contractValidityInfoBitmap[0]) {
                contractValidityEndDate = parseDate(bitUtils.getNextInteger(14), DATE_01_01_1997)
            }
        }

        var contractStatus: Int? = null
        if (bitmap[BITMAP_SIZE - 6]) {
            contractStatus = bitUtils.getNextInteger(8)
        }


        return IntercodePublicTransportContract(
            bitmap = bitmap,
            contractProvider = contractProvider,
            contractPassengerClass = contractPassengerClass,
            contractSerialNumber = contractSerialNumber,
            contractStatus = contractStatus,
            contractTariffval = contractTariffval,
            contractValidityStartDate = contractValidityStartDate,
            contractValidityEndDate = contractValidityEndDate,
            contractValidityInfo = contractValidityInfo
        )
    }

    override fun generate(content: AbstractIntercodeContract): ByteArray {
        TODO("Not yet implemented")
    }

    companion object {
        const val BITMAP_SIZE = 7
    }
}