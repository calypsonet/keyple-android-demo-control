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
package org.eclipse.keyple.demo.control.activities

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_network_invalid.invalidDescription
import kotlinx.android.synthetic.main.activity_network_invalid.invalid_title
import kotlinx.android.synthetic.main.activity_network_invalid.presentBtn
import kotlinx.android.synthetic.main.logo_toolbar.toolbarLogo
import org.eclipse.keyple.demo.control.R
import org.eclipse.keyple.demo.control.activities.CardReaderActivity.Companion.CARD_CONTENT
import org.eclipse.keyple.demo.control.models.CardReaderResponse

class NetworkInvalidActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_invalid)
        setSupportActionBar(findViewById(R.id.toolbar))
        toolbarLogo.setImageResource(R.drawable.ic_logo_white)

        val cardContent: CardReaderResponse? = intent.getParcelableExtra(CARD_CONTENT)
        cardContent?.errorTitle?.let {
            invalid_title.text = it
        }
        invalidDescription.text = cardContent?.errorMessage
        presentBtn.setOnClickListener {
            onBackPressed()
        }
    }
}
