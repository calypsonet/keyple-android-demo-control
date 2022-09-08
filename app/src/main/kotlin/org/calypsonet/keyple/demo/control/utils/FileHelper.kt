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
package org.calypsonet.keyple.demo.control.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.Writer
import timber.log.Timber

object FileHelper {

  fun createDirectory(path: String, name: String): Boolean {
    val file = File(path, name)
    return file.mkdir()
  }

  fun createFile(dirPath: String, name: String, content: String): Boolean {
    var result = false
    val file = File(dirPath, name)
    try {
      result =
          try {
            val fileOutputStream = FileOutputStream(file.absolutePath)

            val out: Writer = OutputStreamWriter(fileOutputStream)
            out.write(content)
            out.close()
            true
          } catch (e: IOException) {
            e.printStackTrace()
            false
          }
    } catch (e: IOException) {
      Timber.e(e)
    }
    return result
  }

  fun loadFileFromAssets(fileName: String, context: Context): String? {
    var content: String? = ""
    try {
      val stream: InputStream = context.assets.open(fileName)
      val size: Int = stream.available()
      val buffer = ByteArray(size)
      stream.read(buffer)
      stream.close()
      content = String(buffer)
    } catch (e: IOException) { // Handle exceptions here
    }
    return content
  }

  fun fileExist(path: String): Boolean {
    return File(path).exists()
  }

  @Suppress("Unused")
  fun folderSize(path: String): Long {
    var length: Long = 0
    val listFiles = File(path).listFiles() ?: arrayOf()
    for (file: File in listFiles) {
      length +=
          if (file.isFile) {
            file.length()
          } else {
            folderSize(file.absolutePath)
          }
    }
    return length
  }

  @Suppress("DEPRECATION")
  @Throws(IOException::class)
  fun getExternalStoragePath(): String {
    if (!isExternalStorageAvailable()) {
      throw IOException("External storage not mounted")
    }
    return Environment.getExternalStorageDirectory().absolutePath
  }

  /** @return `true` if the external storage is mounted and readable/writable; `false` otherwise */
  private fun isExternalStorageAvailable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
  }
}
