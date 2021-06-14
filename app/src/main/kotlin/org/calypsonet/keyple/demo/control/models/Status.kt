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
package org.calypsonet.keyple.demo.control.models

enum class Status(val status: String) {
    LOADING("loading"),
    ERROR("error"),
    TICKETS_FOUND("tickets_found"),
    INVALID_CARD("invalid_card"),
    EMPTY_CARD("empty_card"),
    WRONG_CARD("wrong_card"),
    DEVICE_CONNECTED("device_connected"),
    SUCCESS("success");
}
