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
package org.eclipse.keyple.demo.control

import android.content.Context
import androidx.multidex.MultiDex
import dagger.android.DaggerApplication
import org.eclipse.keyple.demo.control.di.AppComponent
import org.eclipse.keyple.demo.control.di.DaggerAppComponent
import timber.log.Timber
import timber.log.Timber.DebugTree

class Application : DaggerApplication() {

    override fun attachBaseContext(context: Context?) {
        super.attachBaseContext(context)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())
    }

    override fun applicationInjector(): AppComponent? {
        return DaggerAppComponent.builder().application(this).build()
    }
}
