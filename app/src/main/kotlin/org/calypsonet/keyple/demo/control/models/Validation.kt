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
package org.calypsonet.keyple.demo.control.models

import android.os.Parcelable
import java.util.Date
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Validation(
    val name: String,
    val location: Location,
    val destination: String?,
    val date: Date,
    val provider: Int? = null
) : Parcelable
