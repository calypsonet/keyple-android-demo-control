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
package org.calypsonet.keyple.demo.control.exception

class NoSamException : ControlException(TITLE, MESSAGE) {

    companion object {
        const val MESSAGE = "No contact secured element (SAM) is available"
        const val TITLE = "Security error"
    }
}