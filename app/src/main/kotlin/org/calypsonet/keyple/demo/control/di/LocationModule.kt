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
package org.calypsonet.keyple.demo.control.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.calypsonet.keyple.demo.control.data.LocationFileManager
import org.calypsonet.keyple.demo.control.di.scopes.AppScoped

@Module
class LocationModule {

    @Provides
    @AppScoped
    fun providesLocationFileManager(context: Context): LocationFileManager = LocationFileManager(context)
}
