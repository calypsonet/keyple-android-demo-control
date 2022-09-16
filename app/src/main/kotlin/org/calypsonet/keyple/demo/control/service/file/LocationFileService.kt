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
package org.calypsonet.keyple.demo.control.service.file

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import org.calypsonet.keyple.demo.control.service.ticketing.model.Location

class LocationFileService @Inject constructor(context: Context) {

  val locations: List<Location>

  init {
    locations =
        getGson()
            .fromJson(getFileFromResources(context = context), Array<Location>::class.java)
            .toList()
  }

  /** Get file from raw embedded directory */
  private fun getFileFromResources(context: Context): String {
    val resId = context.resources.getIdentifier(LOCATION_FILE_NAME, "raw", context.packageName)
    val inputStream = context.resources.openRawResource(resId)
    return parseFile(inputStream)
  }

  private fun parseFile(inputStream: InputStream): String {
    val sb = StringBuilder()
    var strLine: String?
    try {
      BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
        while (reader.readLine().also { strLine = it } != null) {
          sb.append(strLine)
        }
      }
    } catch (ignore: IOException) { // ignore
    }
    return sb.toString()
  }

  private fun getGson(): Gson {
    val gsonBuilder = GsonBuilder()
    gsonBuilder.disableHtmlEscaping()
    gsonBuilder.setPrettyPrinting()
    gsonBuilder.setLenient()
    return gsonBuilder.create()
  }

  companion object {
    const val LOCATION_FILE_NAME = "locations"
  }
}
