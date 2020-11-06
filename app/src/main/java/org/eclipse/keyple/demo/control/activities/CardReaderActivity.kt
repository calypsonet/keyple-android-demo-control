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
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlinx.android.synthetic.main.activity_card_reader.invalid
import kotlinx.android.synthetic.main.activity_card_reader.valid
import kotlinx.android.synthetic.main.activity_card_reader.loadingAnimation
import org.eclipse.keyple.demo.control.R
import org.eclipse.keyple.demo.control.models.CardReaderResponse
import org.eclipse.keyple.demo.control.models.CardTitle
import org.eclipse.keyple.demo.control.models.Validation

class CardReaderActivity : AppCompatActivity() {
    private lateinit var timer : Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_reader)
        setSupportActionBar(findViewById(R.id.toolbar))

        // TODO: implement Keyple reader

        // TODO: remove when Keyple implemented
        valid.setOnClickListener {
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
                    CardTitle("Season pass", "From 1st April 2020 to 1st August 2020", false)
                )
            )
            intent.putExtra(CardContentActivity.CARD_CONTENT, cardResponse)
            startActivity(intent)
        }
        invalid.setOnClickListener {
            val intent = Intent(this, NetworkInvalidActivity::class.java)
            val cardResponse =
                CardReaderResponse("some invalid card", arrayListOf(), arrayListOf())
            intent.putExtra(NetworkInvalidActivity.CARD_CONTENT, cardResponse)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadingAnimation.playAnimation()
        timer = Timer() // After cancel, need to reinit Timer
        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread { onBackPressed() }
            }
        }, RETURN_DELAY_MS.toLong())
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    companion object {
        private const val RETURN_DELAY_MS = 30000
    }
}