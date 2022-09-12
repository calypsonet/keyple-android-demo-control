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

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import kotlinx.android.synthetic.main.activity_device_selection.*
import org.calypsonet.keyple.demo.control.service.reader.ReaderType
import org.calypsonet.keyple.demo.control.service.ticketing.model.ControlAppSettings

class DeviceSelectionActivity : BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {

    super.onCreate(savedInstanceState, persistentState)

    bluebirdBtn.setOnClickListener {
      ControlAppSettings.readerType = ReaderType.BLUEBIRD
      startActivity(Intent(this, HomeActivity::class.java))
    }
    coppernicBtn.setOnClickListener {
      ControlAppSettings.readerType = ReaderType.COPPERNIC
      startActivity(Intent(this, HomeActivity::class.java))
    }
    famocoBtn.setOnClickListener {
      ControlAppSettings.readerType = ReaderType.FAMOCO
      startActivity(Intent(this, HomeActivity::class.java))
    }
    flowbirdBtn.setOnClickListener {
      ControlAppSettings.readerType = ReaderType.FLOWBIRD
      startActivity(Intent(this, HomeActivity::class.java))
    }
  }
}
