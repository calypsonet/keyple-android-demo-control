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
package org.calypsonet.keyple.parser.intercode.model

import java.util.Date

/**
 *
 *  @author youssefamrani
 */

data class IntercodePublicTransportContract(
    val bitmap: BooleanArray,
    val contractProvider: Int?,
    val contractTariffval: Int?,
    val contractSerialNumber: Int?,
    val contractPassengerClass: Int?,
    val contractValidityInfo: ByteArray?,
    val contractValidityStartDate: Date?,
    val contractValidityEndDate: Date?,
    val contractStatus: Int?
) : AbstractIntercodeContract()
