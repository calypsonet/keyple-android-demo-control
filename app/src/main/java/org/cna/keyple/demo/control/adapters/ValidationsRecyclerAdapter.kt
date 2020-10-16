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

package org.cna.keyple.demo.control.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.validation_recycler_row.view.*
import org.cna.keyple.demo.control.R
import org.cna.keyple.demo.control.inflate
import org.cna.keyple.demo.control.models.Validation
import java.text.SimpleDateFormat
import java.util.*

class ValidationsRecyclerAdapter(
    private val validations: ArrayList<Validation>
): RecyclerView.Adapter<ValidationsRecyclerAdapter.LastValidationHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LastValidationHolder {
        val inflatedView = parent.inflate(R.layout.validation_recycler_row, false)
        return LastValidationHolder(inflatedView)
    }

    class LastValidationHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view: View = v
        private var validation: Validation? = null

        fun bindItem(validation: Validation){
            this.validation = validation
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH)
            view.title_name.text = validation.name
            view.location.text = validation.location
            view.date.text = formatter.format(validation.date)
        }
    }

    override fun getItemCount() = validations.size

    override fun onBindViewHolder(holder: LastValidationHolder, position: Int) {
        val validationItem = validations[position]
        holder.bindItem(validationItem)
    }
}