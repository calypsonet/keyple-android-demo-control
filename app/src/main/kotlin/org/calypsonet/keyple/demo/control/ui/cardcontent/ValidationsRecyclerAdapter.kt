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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.calypsonet.keyple.demo.control.data.model.Validation
import org.calypsonet.keyple.demo.control.databinding.ValidationRecyclerRowBinding

class ValidationsRecyclerAdapter(private val validations: ArrayList<Validation>) :
    RecyclerView.Adapter<ValidationsRecyclerAdapter.LastValidationHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LastValidationHolder {
    val binding =
        ValidationRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return LastValidationHolder(binding)
  }

  class LastValidationHolder(private val binding: ValidationRecyclerRowBinding) :
      RecyclerView.ViewHolder(binding.root) {

    private var validation: Validation? = null

    fun bindItem(validation: Validation) {
      this.validation = validation
      val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy\nHH:mm:ss", Locale.ENGLISH)
      binding.titleLocation.text =
          String.format("%s\n%s", validation.name, validation.location.name)
      binding.date.text = validation.dateTime.format(formatter)
    }
  }

  override fun getItemCount() = validations.size

  override fun onBindViewHolder(holder: LastValidationHolder, position: Int) {
    val validationItem = validations[position]
    holder.bindItem(validationItem)
  }
}
