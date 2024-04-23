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
package org.calypsonet.keyple.demo.control.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.widget.ArrayAdapter
import org.calypsonet.keyple.demo.control.BuildConfig
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.AppSettings
import org.calypsonet.keyple.demo.control.data.model.Location
import org.calypsonet.keyple.demo.control.databinding.ActivitySettingsBinding
import org.calypsonet.keyple.demo.control.databinding.LogoToolbarBinding

class SettingsActivity : BaseActivity() {

  private lateinit var activitySettingsBinding: ActivitySettingsBinding
  private lateinit var logoToolbarBinding: LogoToolbarBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activitySettingsBinding = ActivitySettingsBinding.inflate(layoutInflater)
    logoToolbarBinding = activitySettingsBinding.appBarLayout
    setContentView(activitySettingsBinding.root)
    setSupportActionBar(logoToolbarBinding.toolbar)

    activitySettingsBinding.spinnerLocationList.adapter =
        ArrayAdapter(
            this,
            R.layout.spinner_item_location,
            R.id.spinner_item_text,
            locationRepository.locations)
    activitySettingsBinding.validationPeriodEdit.text =
        Editable.Factory.getInstance().newEditable("10")
    activitySettingsBinding.appVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)
    activitySettingsBinding.timeBtn.setOnClickListener {
      startActivityForResult(Intent(Settings.ACTION_DATE_SETTINGS), 0)
    }
    activitySettingsBinding.startBtn.setOnClickListener {
      AppSettings.location = activitySettingsBinding.spinnerLocationList.selectedItem as Location
      val validationPeriod = activitySettingsBinding.validationPeriodEdit.text.toString()
      if (validationPeriod.isNotBlank()) {
        AppSettings.validationPeriod = validationPeriod.toInt()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
      } else {
        showToast(getString(R.string.msg_location_period_empty))
      }
    }
  }
}
