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

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_card_content.emptyContract
import kotlinx.android.synthetic.main.activity_card_content.lastValidationList
import kotlinx.android.synthetic.main.activity_card_content.lastValidationListContainer
import kotlinx.android.synthetic.main.activity_card_content.presentBtn
import kotlinx.android.synthetic.main.activity_card_content.titlesList
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.activities.CardReaderActivity.Companion.CARD_CONTENT
import org.calypsonet.keyple.demo.control.adapters.TitlesRecyclerAdapter
import org.calypsonet.keyple.demo.control.adapters.ValidationsRecyclerAdapter
import org.calypsonet.keyple.demo.control.models.CardReaderResponse
import org.calypsonet.keyple.demo.control.setDivider

class CardContentActivity : BaseActivity() {
    private lateinit var validationLinearLayoutManager: LinearLayoutManager
    private lateinit var titleLinearLayoutManager: LinearLayoutManager
    private lateinit var validationsAdapter: ValidationsRecyclerAdapter
    private lateinit var titlesAdapter: TitlesRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_content)
        setSupportActionBar(findViewById(R.id.toolbar))

        presentBtn.setOnClickListener {
            onBackPressed()
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val cardContent: CardReaderResponse =
            intent.getParcelableExtra(CARD_CONTENT)

        validationLinearLayoutManager = LinearLayoutManager(this)
        lastValidationList.layoutManager = validationLinearLayoutManager

        titleLinearLayoutManager = LinearLayoutManager(this)
        titlesList.layoutManager = titleLinearLayoutManager

        if(!cardContent.titlesList.isNullOrEmpty()){
            titlesAdapter = TitlesRecyclerAdapter(cardContent.titlesList)
            titlesList.adapter = titlesAdapter
            titlesList.visibility = View.VISIBLE
            emptyContract.visibility = View.GONE
        }
        else{
            titlesList.visibility = View.GONE
            emptyContract.visibility = View.VISIBLE
        }

        if(cardContent.lastValidationsList != null){
            lastValidationListContainer.visibility = View.VISIBLE
            validationsAdapter = ValidationsRecyclerAdapter(cardContent.lastValidationsList)
            lastValidationList.adapter = validationsAdapter
            lastValidationList.setDivider(R.drawable.recycler_view_divider)
        }
        else{
            lastValidationListContainer.visibility = View.GONE
        }
    }
}
