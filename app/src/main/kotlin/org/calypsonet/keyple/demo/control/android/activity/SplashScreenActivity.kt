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
import java.util.Timer
import java.util.TimerTask
import org.calypsonet.keyple.demo.control.R

class SplashScreenActivity : BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    // Make sure this is before calling super.onCreate
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_splashscreen)
    Timer()
        .schedule(
            object : TimerTask() {
              override fun run() {
                if (!isFinishing) {
                  startActivity(Intent(applicationContext, DeviceSelectionActivity::class.java))
                  finish()
                }
              }
            },
            SPLASH_MAX_DELAY_MS.toLong())
  }

  companion object {
    private const val SPLASH_MAX_DELAY_MS = 2000
  }
}
