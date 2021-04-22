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

package org.eclipse.keyple.parser.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import org.eclipse.keyple.parser.model.type.VersionNumberEnum
import org.eclipse.keyple.parser.utils.DateUtils
import java.util.Date

/**
 *
 *  @author youssefamrani
 */
@Parcelize
data class ContractStructureDto(
    val contractVersionNumber: VersionNumberEnum,
    val contractTariff: ContractPriorityEnum,
    val contractSaleDate: Int,
    val contractValidityEndDate: Int,
    val contractSaleSam: Int?,
    val contractSaleCounter: Int?,
    val contractAuthKvc: Int?,
    val contractAuthenticator: Int?
): Parcelable{


    fun getContractSaleDateAsDate(): Date{
        return DateUtils.parseDateStamp(contractSaleDate, DateUtils.DATE_01_01_2010)
    }

    fun getContractValidityEndDateAsDate(): Date{
        return DateUtils.parseDateStamp(contractValidityEndDate, DateUtils.DATE_01_01_2010)
    }
}
