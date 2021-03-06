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
import kotlinx.android.parcel.Parcelize
import org.joda.time.DateTime

@Parcelize
data class Contract(
    val name: String?,
    val valid: Boolean,
    val validationDate: DateTime?,
    val record: Int,
    val expired: Boolean,
    val contractValidityStartDate: DateTime,
    val contractValidityEndDate: DateTime,
    val nbTicketsLeft: Int? = null
) : Parcelable
