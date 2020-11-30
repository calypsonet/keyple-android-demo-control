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

@Parcelize
data class IntercodeBestContract(
    val bitmap: BooleanArray,
    val bestContratNetworkId: Int?,
    val bestContratTariff: Int,
    val bestContractPointer: Int
) : Parcelable{


    /**
     * Getter for BestContratType
     *
     * @return BestContratType
     */
    fun getBestContratType(): Int {
        return bestContratTariff and 0x0FF0 shr PRIORITY_OFFSET
    }

    /**
     * Getter for BestContratKey
     *
     * @return BestContratKey
     */
    fun getBestContratKey(): Int? {
        return bestContratTariff and 0xF000 shr TYPE_OFFSET
    }

    companion object{
        /** Offset de la priorité du contrat  */
        private const val PRIORITY_OFFSET = 4

        /** Offset du type de contrat  */
        private const val TYPE_OFFSET = java.lang.Byte.SIZE + PRIORITY_OFFSET
    }
}
