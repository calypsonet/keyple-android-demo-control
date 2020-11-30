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

package org.eclipse.keyple.demo.control.models.mapper

import org.eclipse.keyple.demo.control.models.Location
import org.eclipse.keyple.parser.model.EventStructureDto

/**
 *
 *  @author youssefamrani
 */

object LocationMapper {
    fun map(locations: List<Location>, event: EventStructureDto): Location {
        return locations.filter {
            event.eventLocation == it.id
        }[0]
    }
}

