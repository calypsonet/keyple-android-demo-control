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
import java.util.Date
import kotlinx.android.parcel.Parcelize
import org.eclipse.keyple.parser.utils.DateUtils
import org.eclipse.keyple.parser.utils.DateUtils.DATE_01_01_2010

@Parcelize
data class EnvironmentHolderStructureDto(
    val envVersionNumber: Int,
    val envApplicationNumber: Int,
    val envIssuingDate: Int,
    val envEndDate: Int,
    val holderCompany: Int?,
    val holderIdNumber: Int?
) : Parcelable {

    fun getEnvIssuingDateAsDate(): Date {
        return DateUtils.parseDateStamp(envIssuingDate, DATE_01_01_2010)
    }

    fun getEnvEndDateAsDate(): Date {
        return DateUtils.parseDateStamp(envEndDate, DATE_01_01_2010)
    }
}
