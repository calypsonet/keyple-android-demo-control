/*
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.keyple.demo.control.mock

import android.content.Context
import org.eclipse.keyple.demo.control.R
import org.eclipse.keyple.demo.control.data.LocationFileManager
import org.eclipse.keyple.demo.control.models.CardReaderResponse
import org.eclipse.keyple.demo.control.models.Contract
import org.eclipse.keyple.demo.control.models.Status
import org.eclipse.keyple.demo.control.models.Validation
import org.eclipse.keyple.parser.model.type.ContractPriorityEnum
import org.eclipse.keyple.parser.utils.DateUtils
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.Locale

/**
 *
 *  @author youssefamrani
 */
object MockUtils {

    fun getMockedResult(context: Context, status: Status): CardReaderResponse? {
        val locations = LocationFileManager(context).getLocations()
        var cardReaderResponse: CardReaderResponse? = null
        when (status) {
            Status.TICKETS_FOUND -> {
                val parser = SimpleDateFormat(DateUtils.yyyy_MM_dd_HH_mm_ss, Locale.getDefault())

                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                cardReaderResponse = CardReaderResponse(
                    Status.TICKETS_FOUND, "valid card",
                    arrayListOf(
                        Validation(
                            name = "Titre",
                            location = locations[0],
                            destination = null,
                            date = parser.parse("2020-05-18T08:27:30")
                        ),
                        Validation(
                            name = "Titre",
                            location = locations[1],
                            destination = null,
                            date = parser.parse("2020-01-14T09:55:00")
                        )
                    ),
                    arrayListOf(
                        Contract(
                            name = ContractPriorityEnum.MULTI_TRIP.value,
                            nbTicketsLeft = 2,
                            validationDate = null,
                            valid = true,
                            record = 1,
                            expired = false,
                            contractValidityStartDate = DateTime(DateUtils.parseDate("01/01/2021")),
                            contractValidityEndDate = DateTime(DateUtils.parseDate("01/01/2030"))
                        ),
                        Contract(
                            name = ContractPriorityEnum.SEASON_PASS.value,
                            valid = false,
                            validationDate = null,
                            record = 2,
                            expired = true,
                            contractValidityStartDate = DateTime(DateUtils.parseDate("01/04/2020")),
                            contractValidityEndDate = DateTime(DateUtils.parseDate("01/08/2020"))
                        )
                    )
                )
            }
            Status.LOADING, Status.ERROR, Status.SUCCESS, Status.INVALID_CARD, Status.EMPTY_CARD -> {
                val error = String.format(
                    context.getString(R.string.card_invalid_aid),
                    context.getString(R.string.card_invalid_default)
                )
                cardReaderResponse =
                    CardReaderResponse(
                        status = status,
                        cardType = null,
                        titlesList = arrayListOf(),
                        errorMessage = error
                    )
            }
            Status.WRONG_CARD -> {
                // Do nothing
            }
            Status.DEVICE_CONNECTED -> {
                // Do nothing
            }
        }

        return cardReaderResponse
    }
}

