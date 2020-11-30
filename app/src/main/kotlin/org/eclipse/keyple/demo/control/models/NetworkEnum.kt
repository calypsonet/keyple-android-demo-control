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

package org.eclipse.keyple.demo.control.models

enum class NetworkEnum(val key: Int, val value: String) {
    NORD_PAS_DE_CALAIS(250000, "NORD PAS DE CALAIS"),
    CENTRE(250072, "CENTRE"),
    ILE_DE_FRANCE(250901, "ILE-DE-FRANCE"),
    RHONE_ALPES(250902, "AUVERGNE RHONE-ALPES"),
    ALSACE(250904, "ALSACE"),
    AUVERGNE(250905, "AUVERGNE"),
    BRETAGNE(250908, "BRETAGNE"),
    FRANCH_COMTE(250911, "FRANCHE-COMTE"),
    NORMANDIE(250912, "HAUTE-NORMANDIE"),
    LANGUEDOC_ROUSSILLON(250913, "LANGUEDOC-ROUSSILLON"),
    LIMOUSIN(250914, "LIMOUSIN"),
    GRAND_EST(250915, "GRAND EST"),
    MIDI_PYRENEES(250916, "MIDI-PYRENEES"),
    PACA(250920, "PROVENCE-ALPES-COTE D AZUR"),
    AQUITAINE(250921, "AQUITAINE"),
    BOURGUOGNE(250907, "BOURGOGNE"),
    MILITARY(250999, "NATIONALE"),
    TEST(250991, "TEST");

    override fun toString(): String {
        return "$key / $value"
    }


    companion object {
        fun findEnumByNetworkId(networkId : Int): NetworkEnum? {
            val values = NetworkEnum.values()
            return values.find {
                it.key == networkId
            }
        }
    }

}
