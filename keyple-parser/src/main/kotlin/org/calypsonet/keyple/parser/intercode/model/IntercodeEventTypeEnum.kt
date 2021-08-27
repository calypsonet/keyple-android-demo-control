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

enum class IntercodeEventTypeEnum constructor(val key: Int, val value: String) {

    VALIDATION_IN(0x01, "validation en entrée"),
    VALIDATION_OUT(0x02, "validation en sortie"),
    PASSAGE(0x03, "passage"),
    FLYING_CONTROL(0x04, "contrôle volant"),
    VALIDATION_CORRESPONDENCE_6H(0x06, "validation en correspondance en entrée"),
    VALIDATION_CORRESPONDENCE(0x07, "validation en correspondance en sortie"),
    RUF(0x08, "RUF"),
    VALIDITY_CANCELATION(0x09, "Annulation de valition"),
    CORRESPONDENCE_PUBLIC_IN(0x0A, "correspondance voie publique entrée"),
    CORRESPONDENCE_PUBLIC_OUT(0x0B, "correspondance voie publique sortie"),
    DISTRIBUTION(0x0D, "distribution"),
    INVALIDATION(0x0F, "invalidation");

    companion object {
        fun findEnumByKey(key: Int): IntercodeEventTypeEnum {
            for (eventTypeEnum in IntercodeEventTypeEnum.values()) {
                if (eventTypeEnum.key == key) {
                    return eventTypeEnum
                }
            }
            throw IllegalStateException("EventTypeEnum is not defined")
        }
    }
}
