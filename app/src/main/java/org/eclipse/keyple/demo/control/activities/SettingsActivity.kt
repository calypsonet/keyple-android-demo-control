/*
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.keyple.demo.control.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.keyple.demo.control.R
import org.eclipse.keyple.demo.control.models.KeypleSettings

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<Button>(R.id.time_btn).setOnClickListener {
            startActivityForResult(Intent (Settings.ACTION_DATE_SETTINGS), 0);
        }

        findViewById<ImageButton>(R.id.start_btn).setOnClickListener {
            KeypleSettings.location = findViewById<EditText>(R.id.location_edit).text.toString()
            val validationPeriod = findViewById<EditText>(R.id.validation_period_edit).text.toString()
            KeypleSettings.validationPeriod = if (validationPeriod.isBlank()) 0 else validationPeriod.toInt()
            if(KeypleSettings.location != null && KeypleSettings.validationPeriod != 0){
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                Toast.makeText(this, R.string.msg_location_period_empty, Toast.LENGTH_LONG).show()
            }
        }

    }
}