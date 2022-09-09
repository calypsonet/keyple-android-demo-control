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
package org.calypsonet.keyple.demo.control.android.di

import dagger.Module
import dagger.Provides
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.calypsonet.keyple.demo.control.android.di.scope.AppScoped
import org.calypsonet.keyple.demo.control.android.rx.SchedulerProvider

@Suppress("unused")
@Module
class SchedulerModule {

  @Provides
  @AppScoped
  fun provideSchedulerProvider(): SchedulerProvider =
      SchedulerProvider(Schedulers.io(), AndroidSchedulers.mainThread())
}
