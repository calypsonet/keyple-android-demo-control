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
package org.calypsonet.keyple.demo.control.data

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import org.calypsonet.keyple.demo.control.models.Location
import org.calypsonet.keyple.demo.control.utils.FileHelper
import timber.log.Timber

/** @author youssefamrani */
class LocationFileManager @Inject constructor(context: Context) {

  private var locationList: List<Location>? = null
  var locationsFromResources: String

  init {
    val sdCardPath = FileHelper.getExternalStoragePath()
    val dirPath = "$sdCardPath$LOCATION_DIRECTORY_PATH"

    val dirExists = FileHelper.fileExist(dirPath)
    if (!dirExists) {
      /*
       * Create App directory
       */
      val dirCreated = FileHelper.createDirectory(sdCardPath, LOCATION_DIRECTORY_PATH)
      Timber.i("Locations file directory created : $dirCreated")
    }

    val fileExists = FileHelper.fileExist(FILE_PATH)
    locationsFromResources = getFileFromResources(context = context)
    if (!fileExists) {
      /*
       * Create location JSON file
       */
      val fileName = "$LOCATION_FILE_NAME.json"
      val fileCreated =
          FileHelper.createFile(
              dirPath = dirPath, name = fileName, content = locationsFromResources)
      Timber.i("Locations file created : $fileCreated")
    }
  }

  fun getLocations(): List<Location> {
    if (locationList == null) {
      locationList =
          try {
            val file = getFileFromSdCard()
            getGson().fromJson(file, Array<Location>::class.java).toList()
          } catch (e: FileNotFoundException) {
            Timber.e(e)
            getGson().fromJson(locationsFromResources, Array<Location>::class.java).toList()
          }
    }
    return locationList!!
  }

  /** Get file from SD Card */
  private fun getFileFromSdCard(): String {
    val file = File(FILE_PATH)
    val inputStream = FileInputStream(file)

    return parseFile(inputStream)
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
    const val LOCATION_DIRECTORY_PATH = "/Keyple Demo Control"
    val FILE_PATH =
        "${Environment.getExternalStorageDirectory().absolutePath}$LOCATION_DIRECTORY_PATH/$LOCATION_FILE_NAME.json"
  }
}
