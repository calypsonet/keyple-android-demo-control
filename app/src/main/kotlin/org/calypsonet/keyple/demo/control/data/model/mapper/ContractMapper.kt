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
package org.calypsonet.keyple.demo.control.data.model.mapper

import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.control.data.model.Contract

object ContractMapper {
  fun map(
      contract: ContractStructure,
      record: Int,
      contractValidated: Boolean,
      contractExpired: Boolean,
      validationDate: LocalDateTime?,
      nbTicketsLeft: Int?
  ): Contract {
    return Contract(
        name = contract.contractTariff.value,
        valid = contractValidated,
        record = record,
        validationDate = validationDate,
        expired = contractExpired,
        contractValidityStartDate = contract.contractSaleDate.getDate(),
        contractValidityEndDate = contract.contractValidityEndDate.getDate(),
        nbTicketsLeft = nbTicketsLeft)
  }
}
