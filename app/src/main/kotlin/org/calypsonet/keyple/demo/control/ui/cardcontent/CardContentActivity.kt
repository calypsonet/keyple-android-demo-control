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
package org.calypsonet.keyple.demo.control.ui.cardcontent

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.databinding.ActivityCardContentBinding
import org.calypsonet.keyple.demo.control.databinding.LogoToolbarBinding
import org.calypsonet.keyple.demo.control.setDivider
import org.calypsonet.keyple.demo.control.ui.BaseActivity
import org.calypsonet.keyple.demo.control.ui.ReaderActivity.Companion.CARD_CONTENT
import timber.log.Timber

class CardContentActivity : BaseActivity() {

  private lateinit var activityCardContentBinding: ActivityCardContentBinding
  private lateinit var logoToolbarBinding: LogoToolbarBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardContentBinding = ActivityCardContentBinding.inflate(layoutInflater)
    logoToolbarBinding = activityCardContentBinding.appBarLayout
    setContentView(activityCardContentBinding.root)
    setSupportActionBar(logoToolbarBinding.toolbar)
    activityCardContentBinding.presentBtn.setOnClickListener { onBackPressed() }
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val cardContent: CardReaderResponse = intent.getParcelableExtra(CARD_CONTENT)!!
    activityCardContentBinding.lastValidationList.layoutManager = LinearLayoutManager(this)
    activityCardContentBinding.titlesList.layoutManager = LinearLayoutManager(this)
    if (cardContent.titlesList.isNotEmpty()) {
      activityCardContentBinding.titlesList.adapter = TitlesRecyclerAdapter(cardContent.titlesList)
      activityCardContentBinding.titlesList.visibility = View.VISIBLE
      activityCardContentBinding.emptyContract.visibility = View.GONE
    } else {
      activityCardContentBinding.titlesList.visibility = View.GONE
      activityCardContentBinding.emptyContract.visibility = View.VISIBLE
    }
    if (cardContent.lastValidationsList != null) {
      activityCardContentBinding.lastValidationListContainer.visibility = View.VISIBLE
      activityCardContentBinding.lastValidationList.adapter =
          ValidationsRecyclerAdapter(cardContent.lastValidationsList)
      activityCardContentBinding.lastValidationList.setDivider(R.drawable.recycler_view_divider)
    } else {
      activityCardContentBinding.lastValidationListContainer.visibility = View.GONE
    }
  }

  override fun onResume() {
    super.onResume()
    if (ticketingService.readersInitialized) {
      ticketingService.stopNfcDetection()
      Timber.d("stopNfcDetection")
    }
  }
}
