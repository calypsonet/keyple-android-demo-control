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

package org.cna.keyple.demo.control.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_card_content.*
import org.cna.keyple.demo.control.R
import org.cna.keyple.demo.control.adapters.TitlesRecyclerAdapter
import org.cna.keyple.demo.control.adapters.ValidationsRecyclerAdapter
import org.cna.keyple.demo.control.models.CardReaderResponse

class CardContentActivity : AppCompatActivity() {
    private lateinit var validationLinearLayoutManager: LinearLayoutManager
    private lateinit var titleLinearLayoutManager: LinearLayoutManager
    private lateinit var validationsAdapter: ValidationsRecyclerAdapter
    private lateinit var titlesAdapter: TitlesRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_content)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<Button>(R.id.present_btn).setOnClickListener {
            onBackPressed();
        }

        validationLinearLayoutManager = LinearLayoutManager(this)
        lastValidationList.layoutManager = validationLinearLayoutManager

        titleLinearLayoutManager = LinearLayoutManager(this)
        titlesList.layoutManager = titleLinearLayoutManager


        val cardContent: CardReaderResponse = intent.getParcelableExtra(NetworkInvalidActivity.CARD_CONTENT)
        validationsAdapter = ValidationsRecyclerAdapter(cardContent.lastValidationsList)
        lastValidationList.adapter = validationsAdapter

        titlesAdapter = TitlesRecyclerAdapter(cardContent.titlesList)
        titlesList.adapter = titlesAdapter
    }

    companion object {
        const val CARD_CONTENT = "cardContent"
    }
}