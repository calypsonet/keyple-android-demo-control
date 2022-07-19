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
package org.calypsonet.keyple.demo.control.models

enum class FileStructureEnum(val key: Int) {
  FILE_STRUCTURE_02H(0x2),
  FILE_STRUCTURE_05H(0x5),
  FILE_STRUCTURE_32H(0x32);

  override fun toString(): String {
    return "Structure ${Integer.toHexString(key)}h"
  }

  companion object {
    fun findEnumByKey(key: Int): FileStructureEnum? {
      val values = values()
      return values.find { it.key == key }
    }
  }
}
