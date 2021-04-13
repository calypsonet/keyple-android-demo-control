/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.demo.control.activities

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
import org.eclipse.keyple.demo.control.BuildConfig
import org.eclipse.keyple.demo.control.R
import org.eclipse.keyple.demo.control.models.KeypleSettings
import org.eclipse.keyple.demo.control.models.Location

class SettingsActivity : BaseActivity() {

    private var mLocationAdapter: ArrayAdapter<Location>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))

        //Init location spinner
        val locations = locationFileManager.getLocations()
        mLocationAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item_location, R.id.spinner_item_text, locations
        )
        spinnerLocationList.adapter = mLocationAdapter

        timeBtn.setOnClickListener {
            startActivityForResult(Intent(Settings.ACTION_DATE_SETTINGS), 0)
        }

        startBtn.setOnClickListener {
            KeypleSettings.location = spinnerLocationList.selectedItem as Location
            val validationPeriod = validationPeriodEdit.text.toString()
            KeypleSettings.validationPeriod =
                if (validationPeriod.isBlank()) 0 else validationPeriod.toInt()
            if (KeypleSettings.location != null && KeypleSettings.validationPeriod != 0) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                Toast.makeText(this, R.string.msg_location_period_empty, Toast.LENGTH_LONG).show()
            }
        }

        app_version.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        if (BuildConfig.DEBUG) {
            validationPeriodEdit.text = Editable.Factory.getInstance().newEditable("90")
        }
    }
}
