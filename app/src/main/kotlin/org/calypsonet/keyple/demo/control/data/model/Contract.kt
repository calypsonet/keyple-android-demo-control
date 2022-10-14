/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.demo.control.data.model

import android.os.Parcelable
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Contract(
    val name: String?,
    val valid: Boolean,
    val validationDate: LocalDateTime?,
    val record: Int,
    val expired: Boolean,
    val contractValidityStartDate: LocalDate,
    val contractValidityEndDate: LocalDate,
    val nbTicketsLeft: Int? = null
) : Parcelable
