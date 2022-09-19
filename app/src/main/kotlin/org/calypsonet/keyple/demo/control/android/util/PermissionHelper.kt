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
package org.calypsonet.keyple.demo.control.android.util

import android.app.Activity
import android.content.pm.PackageManager

object PermissionHelper {

  const val MY_PERMISSIONS_REQUEST_ALL = 1000

  fun checkPermission(activity: Activity, permissions: Array<String>): Boolean {
    val permissionDenied = permissions.filter { !isPermissionGranted(activity, it) }
    if (permissionDenied.isNotEmpty()) {
      val permissionsToAsk = arrayOfNulls<String>(permissionDenied.size)
      for ((position, permission) in permissionDenied.withIndex()) {
        permissionsToAsk[position] = permission
      }
      activity.requestPermissions(permissionsToAsk, MY_PERMISSIONS_REQUEST_ALL)
      return false
    }
    return true
  }

  private fun isPermissionGranted(activity: Activity, permission: String): Boolean {
    return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
  }
}
