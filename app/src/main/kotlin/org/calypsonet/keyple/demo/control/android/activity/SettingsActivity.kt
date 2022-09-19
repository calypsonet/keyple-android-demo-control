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
import android.provider.Settings
import android.text.Editable
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_settings.app_version
import kotlinx.android.synthetic.main.activity_settings.spinnerLocationList
import kotlinx.android.synthetic.main.activity_settings.startBtn
import kotlinx.android.synthetic.main.activity_settings.timeBtn
import kotlinx.android.synthetic.main.activity_settings.validationPeriodEdit
import org.calypsonet.keyple.demo.control.ApplicationSettings
import org.calypsonet.keyple.demo.control.BuildConfig
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.service.ticketing.model.Location

class SettingsActivity : BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    setSupportActionBar(findViewById(R.id.toolbar))
    spinnerLocationList.adapter =
        ArrayAdapter(
            this,
            R.layout.spinner_item_location,
            R.id.spinner_item_text,
            locationFileService.locations)
    app_version.text = getString(R.string.version, BuildConfig.VERSION_NAME)
    if (BuildConfig.DEBUG) {
      validationPeriodEdit.text = Editable.Factory.getInstance().newEditable("90")
    }
    timeBtn.setOnClickListener { startActivityForResult(Intent(Settings.ACTION_DATE_SETTINGS), 0) }
    startBtn.setOnClickListener {
      ApplicationSettings.location = spinnerLocationList.selectedItem as Location
      val validationPeriod = validationPeriodEdit.text.toString()
      if (validationPeriod.isNotBlank()) {
        ApplicationSettings.validationPeriod = validationPeriod.toInt()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
      } else {
        Toast.makeText(this, R.string.msg_location_period_empty, Toast.LENGTH_LONG).show()
      }
    }
  }
}
