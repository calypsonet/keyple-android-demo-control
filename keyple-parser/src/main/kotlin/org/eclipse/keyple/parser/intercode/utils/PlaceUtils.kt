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

package org.eclipse.keyple.parser.intercode.utils

import fr.devnied.bitlib.BitUtils
import fr.devnied.bitlib.BytesUtils
import org.apache.commons.lang3.StringUtils

/**
 *
 *  @author youssefamrani
 */

object PlaceUtils {

    fun getIseNse(eventLocationID: String): String {
        val iseNseCode = getIseNseCode(eventLocationID)

        return iseNseCode
    }

    fun getIseNseCode(pPlaceId: String): String {
        val bitUtils = BitUtils(BytesUtils.fromString(pPlaceId))
        val mIse = getZeroPadString(bitUtils.getNextInteger(7), 2)
        val dIse = getZeroPadString(bitUtils.getNextInteger(5), 2)
        val cIse = getZeroPadString(bitUtils.getNextInteger(4), 2)
        return String.format("%s%s-%s", mIse, dIse, cIse)
    }

    private fun getZeroPadString(value: Int, length: Int): String {
        return StringUtils.leftPad(value.toString(), length, "0")
    }
}