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

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.keyple.demo.control.R
import org.eclipse.keyple.demo.control.models.CardReaderResponse
import org.eclipse.keyple.demo.control.models.CardTitle
import org.eclipse.keyple.demo.control.models.Validation
import java.text.SimpleDateFormat
import java.util.*

class CardReaderActivity : AppCompatActivity() {
    private val timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_reader)
        setSupportActionBar(findViewById(R.id.toolbar))

        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread { onBackPressed() }
            }
        }, RETURN_DELAY_MS.toLong())

        // TODO: implement Keyple reader

        // TODO: remove when Keyple implemented
        findViewById<Button>(R.id.valid).setOnClickListener {
            val intent = Intent(this, CardContentActivity::class.java)
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
            val cardResponse = CardReaderResponse("valid card",
                arrayListOf(
                    Validation("Titre", "Place d'Italie", parser.parse("2020-05-18T08:27:30")),
                    Validation(
                        "Titre",
                        "Place d'Italie",
                        parser.parse("2020-01-14T09:55:00")
                    )
                ),
                arrayListOf(
                    CardTitle("Multi trip", "2 trips left", true),
                    CardTitle("Season pass", "From 1st April 2020 to 1st August 2020", true)
                )
            )
            intent.putExtra(CardContentActivity.CARD_CONTENT, cardResponse)
            startActivity(intent)
        }
        findViewById<Button>(R.id.invalid).setOnClickListener {
            val intent = Intent(this, NetworkInvalidActivity::class.java)
            val cardResponse =
                CardReaderResponse("some invalid card", arrayListOf(), arrayListOf())
            intent.putExtra(NetworkInvalidActivity.CARD_CONTENT, cardResponse)
            startActivity(intent)
        }
    }

    companion object {
        private const val RETURN_DELAY_MS = 30000
    }
}