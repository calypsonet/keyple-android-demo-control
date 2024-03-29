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

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.title_recycler_row.view.titleDescription
import kotlinx.android.synthetic.main.title_recycler_row.view.titleName
import kotlinx.android.synthetic.main.title_recycler_row.view.validImg
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.Contract
import org.calypsonet.keyple.demo.control.inflate

class TitlesRecyclerAdapter(private val titles: ArrayList<Contract>) :
    RecyclerView.Adapter<TitlesRecyclerAdapter.TitleHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleHolder {
    val inflatedView = parent.inflate(R.layout.title_recycler_row, false)
    return TitleHolder(inflatedView)
  }

  class TitleHolder(v: View) : RecyclerView.ViewHolder(v) {

    private var view: View = v
    private var title: Contract? = null

    fun bindItem(contract: Contract) {
      val context = view.context
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
      view.titleName.text = contract.name
      view.titleDescription.text = titleDescription
      view.validImg.setImageResource(if (contract.valid) R.drawable.ic_tick else R.drawable.ic_fail)
    }
  }

  override fun getItemCount() = titles.size

  override fun onBindViewHolder(holder: TitleHolder, position: Int) {
    val titleItem = titles[position]
    holder.bindItem(titleItem)
  }
}
