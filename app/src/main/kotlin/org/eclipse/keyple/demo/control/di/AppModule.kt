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
package org.eclipse.keyple.demo.control.di

import android.content.Context
import dagger.Binds
import dagger.Module
import org.eclipse.keyple.demo.control.Application

@Module
abstract class AppModule {
    // expose Application as an injectable context
    @Binds
    abstract fun bindContext(application: Application): Context
}
