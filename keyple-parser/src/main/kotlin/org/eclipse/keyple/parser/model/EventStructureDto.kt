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
package org.eclipse.keyple.parser.model

import android.os.Parcelable
import java.util.Calendar
import java.util.Date
import kotlinx.android.parcel.Parcelize
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import org.eclipse.keyple.parser.utils.DateUtils
import org.eclipse.keyple.parser.utils.DateUtils.DATE_01_01_2010

/**
 *
 *  @author youssefamrani
 */

@Parcelize
class EventStructureDto(
    val eventVersionNumber: Int,
    val eventDateStamp: Int,
    val eventTimeStamp: Int,
    val eventLocation: Int,
    val eventContractUsed: Int,
    val contractPriority1: ContractPriorityEnum,
    val contractPriority2: ContractPriorityEnum,
    val contractPriority3: ContractPriorityEnum,
    val contractPriority4: ContractPriorityEnum
) : Parcelable {

    fun getEventDateStampAsDate(): Date {
        return DateUtils.parseDateStamp(eventDateStamp, DATE_01_01_2010)
    }

    fun getEventTimeStampAsDate(): Date {
        return DateUtils.parseTimeStamp(eventTimeStamp, DATE_01_01_2010)
    }

    fun getEventDate(): Date {
        val eventDate = DateUtils.parseDateStamp(eventDateStamp, DATE_01_01_2010)
        val calendar: Calendar = Calendar.getInstance()
        calendar.time = eventDate
        calendar.add(Calendar.MINUTE, eventTimeStamp)
        return calendar.time
    }
}
