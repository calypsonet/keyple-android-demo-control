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
package org.eclipse.keyple.parser.intercode.parser

import fr.devnied.bitlib.BitUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.eclipse.keyple.parser.IParser
import org.eclipse.keyple.parser.IParser.Companion.DATE_01_01_1997
import org.eclipse.keyple.parser.IParser.Companion.DD_MM_YYYY
import org.eclipse.keyple.parser.intercode.model.IntercodeEnvironment

class IntercodeEnvParser :
    IParser<IntercodeEnvironment> {

    override fun parse(content: ByteArray): IntercodeEnvironment {

        val bitUtils = BitUtils(content)
        bitUtils.currentBitIndex = 0

        val envApplicationVersionNumber = bitUtils.getNextInteger(ENV_AVN_SIZE)
        val envBitmap = parseBitmap(BITMAP_SIZE, bitUtils)

        var envNetworkId: String? = null
        if (envBitmap[BITMAP_SIZE - 1]) {
            envNetworkId = bitUtils.getNextHexaString(ENV_NETWORK_ID_SIZE)
        }

        var envApplicationIssuerId: Int? = null
        if (envBitmap[BITMAP_SIZE - 2]) {
            envApplicationIssuerId = bitUtils.getNextInteger(ENV_APP_ISSUER_SIZE)
        }

        var envApplicationValidityEndDate: Date? = null
        if (envBitmap[BITMAP_SIZE - 3]) {
            val sdf = SimpleDateFormat(DD_MM_YYYY, Locale.getDefault())
            val calendar: Calendar = Calendar.getInstance()

            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            calendar.time = sdf.parse(DATE_01_01_1997)
            val date = bitUtils.getNextInteger(ENV_APP_VALIDITY_END_DATE_SIZE)
            calendar.add(Calendar.DATE, date)

            envApplicationValidityEndDate = calendar.time
        }

        var envPayMethod: String? = null
        if (envBitmap[BITMAP_SIZE - 4]) {
            envPayMethod = bitUtils.getNextString(ENV_PAY_METHOD_SIZE)
        }

        var envAuthenticator: String? = null
        if (envBitmap[BITMAP_SIZE - 5]) {
            envAuthenticator = bitUtils.getNextHexaString(ENV_AUTHENTICATOR_SIZE)
        }

        var envSelectList: String? = null
        if (envBitmap[BITMAP_SIZE - 6]) {
            envSelectList = bitUtils.getNextString(ENV_SELECT_LIST_SIZE)
        }

        return IntercodeEnvironment(
            envApplicationVersionNumber = envApplicationVersionNumber,
            generalBitmap = envBitmap,
            envNetworkId = envNetworkId,
            envApplicationIssuerId = envApplicationIssuerId,
            envApplicationValidityEndDate = envApplicationValidityEndDate,
            envAuthenticator = envAuthenticator,
            envDataCardStatus = true,
            envPayMethod = envPayMethod,
            envSelectList = envSelectList
        )
    }

    override fun generate(content: IntercodeEnvironment): ByteArray {
        TODO("Not yet implemented")
    }

    companion object {
        const val ENV_AVN_SIZE = 6
        const val BITMAP_SIZE = 7
        const val ENV_NETWORK_ID_SIZE = 24
        const val ENV_APP_ISSUER_SIZE = 8
        const val ENV_APP_VALIDITY_END_DATE_SIZE = 14
        const val ENV_PAY_METHOD_SIZE = 11
        const val ENV_AUTHENTICATOR_SIZE = 16
        const val ENV_SELECT_LIST_SIZE = 32
    }
}
