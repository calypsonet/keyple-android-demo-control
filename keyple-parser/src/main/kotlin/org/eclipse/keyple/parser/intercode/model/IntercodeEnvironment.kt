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
package org.eclipse.keyple.parser.intercode.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
data class IntercodeEnvironment(
    val envApplicationVersionNumber: Int,
    val generalBitmap: BooleanArray,
    val envNetworkId: String?,
    val envApplicationIssuerId: Int?,
    val envApplicationValidityEndDate: Date?,
    val envPayMethod: String?,
    val envAuthenticator: String?,
    val envSelectList: String?,
    val envDataCardStatus: Boolean
) : Parcelable{

    fun getIntercodeVersion(): Int {
        return envApplicationVersionNumber and 0x38 shr APPLICATION_VERSION_SIZE
    }

    /** {@inheritDoc}  */
    fun getApplicationVersion(): Int {
        return envApplicationVersionNumber and 0x07
    }

    companion object{
        /** Size in bit of Application Version  */
        private const val APPLICATION_VERSION_SIZE = 3
    }
}
