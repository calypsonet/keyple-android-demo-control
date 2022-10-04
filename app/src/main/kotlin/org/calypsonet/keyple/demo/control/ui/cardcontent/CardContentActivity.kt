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
import kotlinx.android.synthetic.main.activity_card_content.emptyContract
import kotlinx.android.synthetic.main.activity_card_content.lastValidationList
import kotlinx.android.synthetic.main.activity_card_content.lastValidationListContainer
import kotlinx.android.synthetic.main.activity_card_content.presentBtn
import kotlinx.android.synthetic.main.activity_card_content.titlesList
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.setDivider
import org.calypsonet.keyple.demo.control.ui.BaseActivity
import org.calypsonet.keyple.demo.control.ui.ReaderActivity.Companion.CARD_CONTENT
import timber.log.Timber

class CardContentActivity : BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_card_content)
    setSupportActionBar(findViewById(R.id.toolbar))
    presentBtn.setOnClickListener { onBackPressed() }
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val cardContent: CardReaderResponse = intent.getParcelableExtra(CARD_CONTENT)
    lastValidationList.layoutManager = LinearLayoutManager(this)
    titlesList.layoutManager = LinearLayoutManager(this)
    if (cardContent.titlesList.isNotEmpty()) {
      titlesList.adapter = TitlesRecyclerAdapter(cardContent.titlesList)
      titlesList.visibility = View.VISIBLE
      emptyContract.visibility = View.GONE
    } else {
      titlesList.visibility = View.GONE
      emptyContract.visibility = View.VISIBLE
    }
    if (cardContent.lastValidationsList != null) {
      lastValidationListContainer.visibility = View.VISIBLE
      lastValidationList.adapter = ValidationsRecyclerAdapter(cardContent.lastValidationsList)
      lastValidationList.setDivider(R.drawable.recycler_view_divider)
    } else {
      lastValidationListContainer.visibility = View.GONE
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
