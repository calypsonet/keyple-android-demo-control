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
package org.calypsonet.keyple.demo.control.ui.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.calypsonet.keyple.demo.control.ui.activity.*
import org.calypsonet.keyple.demo.control.ui.di.scope.ActivityScoped

@Suppress("unused")
@Module
abstract class UIModule {

  @ActivityScoped @ContributesAndroidInjector abstract fun splashScreenActivity(): MainActivity

  @ActivityScoped @ContributesAndroidInjector abstract fun settingsActivity(): SettingsActivity

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun deviceSelectionActivity(): DeviceSelectionActivity

  @ActivityScoped @ContributesAndroidInjector abstract fun homeActivity(): HomeActivity

  @ActivityScoped @ContributesAndroidInjector abstract fun readerActivity(): ReaderActivity

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun cardContentActivity(): CardContentActivity

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun networkInvalidActivity(): NetworkInvalidActivity
}
