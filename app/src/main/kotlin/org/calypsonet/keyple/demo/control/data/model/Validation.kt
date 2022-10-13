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
import java.time.LocalDateTime
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Validation(
    val name: String,
    val location: Location,
    val destination: String?,
    val date: LocalDateTime,
    val provider: Int? = null
) : Parcelable
