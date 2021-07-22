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
package org.eclipse.keyple.parser

import fr.devnied.bitlib.BitUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 *
 *  @author youssefamrani
 */

interface IParser<T> {

    fun parse(content: ByteArray): T

    fun generate(content: T): ByteArray

    fun parseBitmap(size: Int, bitUtils: BitUtils): BooleanArray {
        val bitmap = BooleanArray(size)
        for (i in 0 until size) {
            bitmap[i] = bitUtils.nextBoolean
        }
        return bitmap
    }

    fun parseDate(content: Int, startDate: String): Date {

        val sdf =
            SimpleDateFormat(DD_MM_YYYY, Locale.getDefault())
        val calendar: Calendar = Calendar.getInstance()
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        calendar.time = sdf.parse(startDate)

        calendar.add(Calendar.DATE, content)
        return calendar.time
    }

    fun parseMinutes(date: Date, content: Int): Date {
        val calendar: Calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MINUTE, content)
        return calendar.time
    }

    companion object {
        const val DATE_01_01_1997 = "01/01/1997"
        const val DATE_01_01_2010 = "01/01/2010"
        const val DD_MM_YYYY = "dd/MM/yyyy"
    }
}
