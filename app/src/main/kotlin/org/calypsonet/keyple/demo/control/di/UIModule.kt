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
package org.calypsonet.keyple.demo.control.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.calypsonet.keyple.demo.control.activities.CardContentActivity
import org.calypsonet.keyple.demo.control.activities.CardReaderActivity
import org.calypsonet.keyple.demo.control.activities.HomeActivity
import org.calypsonet.keyple.demo.control.activities.NetworkInvalidActivity
import org.calypsonet.keyple.demo.control.activities.SettingsActivity
import org.calypsonet.keyple.demo.control.activities.SplashScreenActivity
import org.calypsonet.keyple.demo.control.di.scopes.ActivityScoped

@Module
abstract class UIModule {
  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun splashScreenActivity(): SplashScreenActivity

  @ActivityScoped @ContributesAndroidInjector abstract fun settingsActivity(): SettingsActivity

  @ActivityScoped @ContributesAndroidInjector abstract fun homeActivity(): HomeActivity

  @ActivityScoped @ContributesAndroidInjector abstract fun cardReaderActivity(): CardReaderActivity

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun cardContentActivity(): CardContentActivity

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun networkInvalidActivity(): NetworkInvalidActivity
}
