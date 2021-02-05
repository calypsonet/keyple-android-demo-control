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

package org.eclipse.keyple.parser.model.type

/**
 *
 *  @author youssefamrani
 */

enum class ContractPriorityEnum constructor(val key: Int, val value: String) {
    FORBIDDEN(0, "Forbidden"),
    SEASON_PASS(1, "Season Pass"),
    MULTI_TRIP(2, "Multi-trip ticket"),
    STORED_VALUE(3, "Stored Value"),
    EXPIRED(31, "Expired"),
    UNKNOWN(-1, "Unknown");

    companion object {
        fun findEnumByKey(key: Int): ContractPriorityEnum {
            for (contractPriorityEnum in ContractPriorityEnum.values()) {
                if (contractPriorityEnum.key == key) {
                    return contractPriorityEnum
                }
            }
            return UNKNOWN
//            throw IllegalStateException("ContractPriorityEnum is not defined")
        }
    }
}