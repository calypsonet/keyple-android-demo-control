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
package org.calypsonet.keyple.demo.control.android.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.android.synthetic.main.validation_recycler_row.view.date
import kotlinx.android.synthetic.main.validation_recycler_row.view.titleLocation
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.inflate
import org.calypsonet.keyple.demo.control.service.ticketing.model.Validation

class ValidationsRecyclerAdapter(private val validations: ArrayList<Validation>) :
    RecyclerView.Adapter<ValidationsRecyclerAdapter.LastValidationHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LastValidationHolder {
    val inflatedView = parent.inflate(R.layout.validation_recycler_row, false)
    return LastValidationHolder(inflatedView)
  }

  class LastValidationHolder(v: View) : RecyclerView.ViewHolder(v) {
    private var view: View = v
    private var validation: Validation? = null

    fun bindItem(validation: Validation) {
      this.validation = validation
      val formatter = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH)
      view.titleLocation.text = String.format("%s - %s", validation.name, validation.location.name)
      view.date.text = formatter.format(validation.date)
    }
  }

  override fun getItemCount() = validations.size

  override fun onBindViewHolder(holder: LastValidationHolder, position: Int) {
    val validationItem = validations[position]
    holder.bindItem(validationItem)
  }
}
