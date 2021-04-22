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

enum class VersionNumberEnum constructor(val key: Int, val value: String) {
    UNDEFINED(0, "Undefined (Forbidden)"),
    CURRENT_VERSION(1, "Current version"),
    RESERVED(255, "Reserved (Forbidden)"),
    UNKNOWN(-1, "Unknown");

    companion object {
        fun findEnumByKey(key: Int): VersionNumberEnum {
            for (versionNumberEnum in VersionNumberEnum.values()) {
                if (versionNumberEnum.key == key) {
                    return versionNumberEnum
                }
            }
            return UNKNOWN
//            throw IllegalStateException("VersionNumberEnum is not defined")
        }
    }
}