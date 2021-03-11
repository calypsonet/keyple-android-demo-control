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
package org.calypsonet.keyple.demo.control.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.locationSelected
import kotlinx.android.synthetic.main.activity_home.startBtn
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.models.KeypleSettings

class HomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(findViewById(R.id.toolbar))
        locationSelected.text = KeypleSettings.location?.toString() ?: ""

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        startBtn.setOnClickListener {
            startActivity(Intent(this, CardReaderActivity::class.java))
        }
    }


    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if(menuItem.itemId == android.R.id.home){
            finish()
        }

        return super.onOptionsItemSelected(menuItem)
    }
}
