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
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.keyple.demo.control.R
import org.eclipse.keyple.demo.control.models.CardReaderResponse

class NetworkInvalidActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_invalid)
        setSupportActionBar(findViewById(R.id.toolbar))

        val cardContent: CardReaderResponse? = intent.getParcelableExtra(CARD_CONTENT)
        findViewById<TextView>(R.id.invalid_description).text =
            String.format(getString(R.string.card_invalid_desc), cardContent?.cardType?: getString(R.string.card_invalid_default))
        findViewById<Button>(R.id.present_btn).setOnClickListener {
            onBackPressed();
        }
    }

    companion object {
        const val CARD_CONTENT = "cardContent"
    }
}