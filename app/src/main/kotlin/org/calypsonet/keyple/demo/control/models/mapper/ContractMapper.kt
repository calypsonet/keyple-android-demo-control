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
package org.calypsonet.keyple.demo.control.models.mapper

import org.calypsonet.keyple.demo.common.parser.model.CardContract
import org.calypsonet.keyple.demo.control.models.Contract
import org.joda.time.DateTime

object ContractMapper {
  fun map(
      contract: CardContract,
      record: Int,
      contractValidated: Boolean,
      contractExpired: Boolean,
      validationDate: DateTime?,
      nbTicketsLeft: Int?
  ): Contract {

    return Contract(
        name = contract.contractTariff.value,
        valid = contractValidated,
        record = record,
        validationDate = validationDate,
        expired = contractExpired,
        contractValidityStartDate = DateTime(contract.getContractSaleDateAsDate()),
        contractValidityEndDate = DateTime(contract.getContractValidityEndDateAsDate()),
        nbTicketsLeft = nbTicketsLeft)
  }
}
