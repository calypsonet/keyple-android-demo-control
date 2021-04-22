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

/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.parser.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class DateUtilsTest {

    @Test
    fun dateToDateCompact() {
        val calendar = Calendar.getInstance()

        calendar.set(2010, 0, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val date1 = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, 2)
        val date2 = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, 3)
        val date3 = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, 4)
        val date4 = calendar.time

        val nb1 = DateUtils.dateToDateCompact(date1)
        val nb2 = DateUtils.dateToDateCompact(date2)
        val nb3 = DateUtils.dateToDateCompact(date3)
        val nb4 = DateUtils.dateToDateCompact(date4)

        assertEquals(0, nb1)
        assertEquals(1, nb2)
        assertEquals(2, nb3)
        assertEquals(3, nb4)
    }

    @Test
    fun dateToTimeCompact() {

        val calendar = Calendar.getInstance()

        calendar.set(2021, Calendar.JANUARY, 14, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val date1 = calendar.time

        calendar.set(Calendar.MINUTE, 1)
        val date2 = calendar.time

        calendar.set(Calendar.MINUTE, 2)
        val date3 = calendar.time

        calendar.set(Calendar.MINUTE, 3)
        val date4 = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        val date5 = calendar.time

        val nb1 = DateUtils.dateToTimeCompact(date1)
        val nb2 = DateUtils.dateToTimeCompact(date2)
        val nb3 = DateUtils.dateToTimeCompact(date3)
        val nb4 = DateUtils.dateToTimeCompact(date4)
        val nb5 = DateUtils.dateToTimeCompact(date5)

        assertEquals(0, nb1)
        assertEquals(1, nb2)
        assertEquals(2, nb3)
        assertEquals(3, nb4)
        assertEquals(60, nb5)
    }

}
