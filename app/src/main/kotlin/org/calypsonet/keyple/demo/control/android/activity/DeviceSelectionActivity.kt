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
package org.calypsonet.keyple.demo.control.android.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_device_selection.*
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.android.dialog.PermissionDeniedDialog
import org.calypsonet.keyple.demo.control.android.util.PermissionHelper
import org.calypsonet.keyple.demo.control.service.reader.ReaderType
import org.calypsonet.keyple.demo.control.service.ticketing.model.ControlAppSettings
import org.calypsonet.keyple.plugin.bluebird.BluebirdPlugin
import org.calypsonet.keyple.plugin.coppernic.Cone2Plugin
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoPlugin
import org.calypsonet.keyple.plugin.flowbird.FlowbirdPlugin

class DeviceSelectionActivity : BaseActivity() {

  private val mock: String = "Mock"
  private val permissions: MutableList<String> = mutableListOf()

  override fun onCreate(savedInstanceState: Bundle?) {

    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_device_selection)

    if (BluebirdPlugin.PLUGIN_NAME.contains(mock)) {
      bluebirdBtn.setBackgroundColor(Color.GRAY)
    } else {
      bluebirdBtn.setOnClickListener {
        ControlAppSettings.readerType = ReaderType.BLUEBIRD
        permissions.addAll(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                "com.bluebird.permission.SAM_DEVICE_ACCESS"))
        val granted = PermissionHelper.checkPermission(this, permissions.toTypedArray())
        if (granted) {
          startActivity(Intent(this, SettingsActivity::class.java))
          finish()
        }
      }
    }

    if (Cone2Plugin.PLUGIN_NAME.contains(mock)) {
      coppernicBtn.setBackgroundColor(Color.GRAY)
    } else {
      coppernicBtn.setOnClickListener {
        ControlAppSettings.readerType = ReaderType.COPPERNIC
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
      }
    }

    if (AndroidFamocoPlugin.PLUGIN_NAME.contains(mock)) {
      famocoBtn.setBackgroundColor(Color.GRAY)
    } else {
      famocoBtn.setOnClickListener {
        ControlAppSettings.readerType = ReaderType.FAMOCO
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
      }
    }

    if (FlowbirdPlugin.PLUGIN_NAME.contains(mock)) {
      flowbirdBtn.setBackgroundColor(Color.GRAY)
    } else {
      flowbirdBtn.setOnClickListener {
        ControlAppSettings.readerType = ReaderType.FLOWBIRD
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
      }
    }
  }

  override fun onDestroy() {
    val granted = PermissionHelper.checkPermission(this, permissions.toTypedArray())
    if (granted) {
      startActivity(Intent(this, SettingsActivity::class.java))
    }
    super.onDestroy()
  }

  @SuppressLint("MissingSuperCall")
  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
  ) {
    when (requestCode) {
      PermissionHelper.MY_PERMISSIONS_REQUEST_ALL -> {
        if (grantResults.isNotEmpty()) {
          for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
              PermissionDeniedDialog().apply {
                show(supportFragmentManager, PermissionDeniedDialog::class.java.simpleName)
              }
              return
            }
          }
          startActivity(Intent(applicationContext, SettingsActivity::class.java))
          finish()
        } else {
          PermissionDeniedDialog().apply {
            show(supportFragmentManager, PermissionDeniedDialog::class.java.simpleName)
          }
        }
        return
      }
      // Add other 'when' lines to check for other
      // permissions this app might request.
      else -> {
        // Ignore all other requests.
      }
    }
  }
}
