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

package org.eclipse.keyple.parser.intercode.model

enum class IntercodeEventTransportEnum constructor(val key: Int, val value: String) {

    UNSPECIFIED(0x0, "Non spécifié"),
    BUS(0x01, "Bus"),
    BUS_INTERCITY(0x02, "Bus interurbain"),
    SUBWAY(0x03, "Métro"),
    TRAMWAY(0x04, "Tramway"),
    TRAIN(0x05, "Train"),
    PARKING(0x08, "Parking"),
    VAL(0x0E, "VAL");

    companion object {
        fun findEnumByKey(key: Int): IntercodeEventTransportEnum {
            for (eventTransportEnum in IntercodeEventTransportEnum.values()) {
                if (eventTransportEnum.key == key) {
                    return eventTransportEnum
                }
            }
            throw IllegalStateException("eventTransportEnum is not defined")
        }
    }
}