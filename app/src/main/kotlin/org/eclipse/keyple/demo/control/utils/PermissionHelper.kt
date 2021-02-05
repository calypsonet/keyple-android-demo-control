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

package org.eclipse.keyple.demo.control.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.ArrayList

/**
 * @author youssefamrani
 */
object PermissionHelper {

    const val MY_PERMISSIONS_REQUEST_ALL = 1000

    fun isPermissionGranted(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkPermission(context: Activity, permissions: Array<String>): Boolean {
        val permissionDenied =
            ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionDenied.add(permission)
            }
        }
        if (!permissionDenied.isEmpty()) {
            var position = 0
            val permissionsToAsk =
                arrayOfNulls<String>(permissionDenied.size)
            for (permission in permissionDenied) {
                permissionsToAsk[position] = permission
                position++
            }
            ActivityCompat.requestPermissions(
                context,
                permissionsToAsk,
                MY_PERMISSIONS_REQUEST_ALL
            )
            return false
        }
        return true
    }
}