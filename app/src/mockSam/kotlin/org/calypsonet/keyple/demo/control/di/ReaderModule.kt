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
import dagger.Provides
import org.calypsonet.keyple.demo.control.di.scopes.AppScoped
import org.calypsonet.keyple.demo.control.reader.IReaderRepository
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi
import timber.log.Timber

@Suppress("unused")
@Module
class ReaderModule {

  @Provides
  @AppScoped
  fun provideReaderRepository(
      readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
  ): IReaderRepository = MockSamReaderRepositoryImpl(readerObservationExceptionHandler)

  @Provides
  @AppScoped
  fun provideReaderObservationExceptionHandler(): CardReaderObservationExceptionHandlerSpi =
      CardReaderObservationExceptionHandlerSpi { pluginName, readerName, e ->
        Timber.e("An unexpected reader error occurred: $pluginName:$readerName : $e")
      }
}
