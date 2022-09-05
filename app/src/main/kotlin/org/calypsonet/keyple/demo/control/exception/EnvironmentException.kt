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
package org.calypsonet.keyple.demo.control.exception

class EnvironmentControlException(key: EnvironmentControlExceptionKey) :
    ControlException(key.title, key.message)

enum class EnvironmentControlExceptionKey
constructor(val key: Int, val title: String, val message: String) {
  WRONG_VERSION_NUMBER(0, "Invalid on this network", "Environment Error: wrong version number"),
  EXPIRED(1, "Invalid on this network", "Environment Error: end date expired")
}
