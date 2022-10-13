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
package org.calypsonet.keyple.demo.control.ui.deviceselection

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.calypsonet.keyple.demo.control.R

class EnableNfcDialog : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return activity?.let {
      // Use the Builder class for convenient dialog construction
      val builder = AlertDialog.Builder(it)
      builder.setCancelable(false).setMessage(R.string.nfc_not_enabled).setPositiveButton(
          android.R.string.ok) { _, _ -> dismiss() }
      // Create the AlertDialog object and return it
      builder.create()
    }
        ?: throw IllegalStateException("Activity cannot be null")
  }
}
