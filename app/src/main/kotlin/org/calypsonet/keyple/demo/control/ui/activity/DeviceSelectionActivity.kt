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
package org.calypsonet.keyple.demo.control.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_device_selection.*
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.AppSettings
import org.calypsonet.keyple.demo.control.data.model.ReaderType
import org.calypsonet.keyple.demo.control.ui.dialog.PermissionDeniedDialog
import org.calypsonet.keyple.demo.control.ui.util.PermissionHelper
import org.calypsonet.keyple.plugin.bluebird.BluebirdPlugin
import org.calypsonet.keyple.plugin.flowbird.FlowbirdPlugin

class DeviceSelectionActivity : BaseActivity() {

  private val mock: String = "Mock"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_device_selection)
    // Bluebird
    if (BluebirdPlugin.PLUGIN_NAME.contains(mock)) {
      bluebirdBtn.setBackgroundColor(Color.GRAY)
    } else {
      bluebirdBtn.setOnClickListener {
        AppSettings.readerType = ReaderType.BLUEBIRD
        val permissions: MutableList<String> =
            mutableListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                "com.bluebird.permission.SAM_DEVICE_ACCESS")
        val granted = PermissionHelper.checkPermission(this, permissions.toTypedArray())
        if (granted) {
          startActivity(Intent(this, SettingsActivity::class.java))
          finish()
        }
      }
    }
    // Coppernic
    coppernicBtn.setOnClickListener {
      AppSettings.readerType = ReaderType.COPPERNIC
      startActivity(Intent(this, SettingsActivity::class.java))
      finish()
    }
    // Famoco
    famocoBtn.setOnClickListener {
      AppSettings.readerType = ReaderType.FAMOCO
      startActivity(Intent(this, SettingsActivity::class.java))
      finish()
    }
    // Flowbird
    if (FlowbirdPlugin.PLUGIN_NAME.contains(mock)) {
      flowbirdBtn.setBackgroundColor(Color.GRAY)
    } else {
      flowbirdBtn.setOnClickListener {
        AppSettings.readerType = ReaderType.FLOWBIRD
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
      }
    }
    // Standard NFC terminal
    nfcTerminalBtn.setOnClickListener {
      AppSettings.readerType = ReaderType.NFC_TERMINAL
      startActivity(Intent(this, SettingsActivity::class.java))
      finish()
    }
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
