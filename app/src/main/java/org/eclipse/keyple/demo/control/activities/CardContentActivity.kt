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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_card_content.lastValidationList
import kotlinx.android.synthetic.main.activity_card_content.presentBtn
import kotlinx.android.synthetic.main.activity_card_content.titlesList
import org.eclipse.keyple.demo.control.R
import org.eclipse.keyple.demo.control.adapters.TitlesRecyclerAdapter
import org.eclipse.keyple.demo.control.adapters.ValidationsRecyclerAdapter
import org.eclipse.keyple.demo.control.models.CardReaderResponse
import org.eclipse.keyple.demo.control.setDivider


class CardContentActivity : AppCompatActivity() {
    private lateinit var validationLinearLayoutManager: LinearLayoutManager
    private lateinit var titleLinearLayoutManager: LinearLayoutManager
    private lateinit var validationsAdapter: ValidationsRecyclerAdapter
    private lateinit var titlesAdapter: TitlesRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_content)
        setSupportActionBar(findViewById(R.id.toolbar))

        presentBtn.setOnClickListener {
            onBackPressed();
        }

        validationLinearLayoutManager = LinearLayoutManager(this)
        lastValidationList.layoutManager = validationLinearLayoutManager

        titleLinearLayoutManager = LinearLayoutManager(this)
        titlesList.layoutManager = titleLinearLayoutManager


        val cardContent: CardReaderResponse = intent.getParcelableExtra(NetworkInvalidActivity.CARD_CONTENT)
        validationsAdapter = ValidationsRecyclerAdapter(cardContent.lastValidationsList)
        lastValidationList.adapter = validationsAdapter
        lastValidationList.setDivider(R.drawable.recycler_view_divider)


        titlesAdapter = TitlesRecyclerAdapter(cardContent.titlesList)
        titlesList.adapter = titlesAdapter
    }

    companion object {
        const val CARD_CONTENT = "cardContent"
    }
}