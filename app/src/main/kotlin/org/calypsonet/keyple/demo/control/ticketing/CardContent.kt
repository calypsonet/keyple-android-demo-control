/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.calypsonet.keyple.demo.control.ticketing

import java.util.SortedMap
import java.util.TreeMap
import org.eclipse.keyple.core.util.ByteArrayUtil
import org.calypsonet.keyple.demo.control.models.StructureEnum

class CardContent {
    var serialNumber: ByteArray? = null
    var poRevision: String? = null
    var poTypeName: String? = null
    var poStructure: Byte? = null
    var extraInfo: String? = null
    var icc: SortedMap<Int, ByteArray>
    var id: SortedMap<Int, ByteArray>
    var environment: SortedMap<Int, ByteArray>
    var eventLog: SortedMap<Int, ByteArray>
    var specialEvents: SortedMap<Int, ByteArray>
    var contractsList: SortedMap<Int, ByteArray>
    var odMemory: SortedMap<Int, ByteArray>
    var contracts: SortedMap<Int, ByteArray>
    var counters: SortedMap<Int, ByteArray>

    init {
        counters = TreeMap()
        contracts = TreeMap()
        odMemory = TreeMap()
        contractsList = TreeMap()
        specialEvents = TreeMap()
        eventLog = TreeMap()
        environment = TreeMap()
        id = TreeMap()
        icc = TreeMap()
    }

    override fun toString(): String {
        return ("SN : " + (if (serialNumber != null) ByteArrayUtil.toHex(serialNumber) else "null") +
                "- PoTypeName:" + poTypeName + " - Ticket available:" +
                (if (counters.size > 0) counters[1] else "empty") + " - Contracts available : " +
                if (contracts.size > 0) String(contracts[1]!!) else "empty")
    }
}
