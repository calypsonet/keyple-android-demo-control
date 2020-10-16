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
import kotlinx.android.synthetic.main.title_recycler_row.view.*
import kotlinx.android.synthetic.main.validation_recycler_row.view.title_name
import org.cna.keyple.demo.control.R
import org.cna.keyple.demo.control.inflate
import org.cna.keyple.demo.control.models.CardTitle

class TitlesRecyclerAdapter(
    private val titles: ArrayList<CardTitle>
): RecyclerView.Adapter<TitlesRecyclerAdapter.TitleHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TitleHolder {
        val inflatedView = parent.inflate(R.layout.title_recycler_row, false)
        return TitleHolder(inflatedView)
    }

    class TitleHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view: View = v
        private var title: CardTitle? = null

        fun bindItem(title: CardTitle){
            this.title = title
            view.title_name.text = title.name
            view.title_description.text = title.description
        }
    }

    override fun getItemCount() = titles.size

    override fun onBindViewHolder(holder: TitleHolder, position: Int) {
        val titleItem = titles[position]
        holder.bindItem(titleItem)
    }
}