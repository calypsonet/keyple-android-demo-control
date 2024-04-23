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

import android.os.Bundle
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.databinding.ActivityNetworkInvalidBinding
import org.calypsonet.keyple.demo.control.databinding.LogoToolbarBinding
import org.calypsonet.keyple.demo.control.ui.ReaderActivity.Companion.CARD_CONTENT
import timber.log.Timber

class NetworkInvalidActivity : BaseActivity() {
  private lateinit var activityNetworkInvalidBinding: ActivityNetworkInvalidBinding
  private lateinit var logoToolbarBinding: LogoToolbarBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityNetworkInvalidBinding = ActivityNetworkInvalidBinding.inflate(layoutInflater)
    logoToolbarBinding = activityNetworkInvalidBinding.appBarLayout
    setContentView(activityNetworkInvalidBinding.root)
    setSupportActionBar(logoToolbarBinding.toolbar)
    logoToolbarBinding.toolbarLogo.setImageResource(R.drawable.ic_logo_white)
    val cardContent: CardReaderResponse? = intent.getParcelableExtra(CARD_CONTENT)
    cardContent?.errorTitle?.let { activityNetworkInvalidBinding.invalidTitle.text = it }
    activityNetworkInvalidBinding.invalidDescription.text = cardContent?.errorMessage
    activityNetworkInvalidBinding.presentBtn.setOnClickListener { onBackPressed() }
  }

  override fun onResume() {
    super.onResume()
    if (ticketingService.readersInitialized) {
      ticketingService.stopNfcDetection()
      Timber.d("stopNfcDetection")
    }
  }
}
