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

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_home.locationSelected
import kotlinx.android.synthetic.main.activity_home.startBtn
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.AppSettings

class HomeActivity : BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_home)
    setSupportActionBar(findViewById(R.id.toolbar))
    locationSelected.text = AppSettings.location.toString()
    startBtn.setOnClickListener { startActivity(Intent(this, ReaderActivity::class.java)) }
  }
}
