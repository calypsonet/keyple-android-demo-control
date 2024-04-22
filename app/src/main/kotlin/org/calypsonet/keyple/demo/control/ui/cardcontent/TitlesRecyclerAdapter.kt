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
import java.util.*
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.Contract
import org.calypsonet.keyple.demo.control.databinding.TitleRecyclerRowBinding

class TitlesRecyclerAdapter(private val titles: ArrayList<Contract>) :
    RecyclerView.Adapter<TitlesRecyclerAdapter.TitleHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleHolder {
    val binding =
        TitleRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return TitleHolder(binding)
  }

  class TitleHolder(private val binding: TitleRecyclerRowBinding) :
      RecyclerView.ViewHolder(binding.root) {

    private var title: Contract? = null

    fun bindItem(contract: Contract) {
      val context = binding.root.context
      val titleDescription =
          if (contract.name == PriorityCode.SEASON_PASS.value) {
            val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
            context.getString(
                R.string.card_content_description_season_pass,
                contract.contractValidityStartDate.format(formatter),
                contract.contractValidityEndDate.format(formatter))
          } else {
            when (val nbTicketsLeft = contract.nbTicketsLeft ?: 0) {
              0 -> context.getString(R.string.card_content_description_multi_trip_zero)
              1 ->
                  context.getString(
                      R.string.card_content_description_multi_trip_single, nbTicketsLeft)
              else ->
                  context.getString(
                      R.string.card_content_description_multi_trip_multiple, nbTicketsLeft)
            }
          }
      this.title = contract
      binding.titleName.text = contract.name
      binding.titleDescription.text = titleDescription
      binding.validImg.setImageResource(
          if (contract.valid) R.drawable.ic_tick else R.drawable.ic_fail)
    }
  }

  override fun getItemCount() = titles.size

  override fun onBindViewHolder(holder: TitleHolder, position: Int) {
    val titleItem = titles[position]
    holder.bindItem(titleItem)
  }
}
