package com.hacybeyker.cartera.core.data.pinpad.di

import com.hacybeyker.cartera.core.data.pinpad.PinPadSeedSource
import com.hacybeyker.cartera.core.data.pinpad.SourcedPinPadSeed
import com.hacybeyker.cartera.core.domain.pinpad.PinPadSeed
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class PinPadSeedModule {

    @Binds
    abstract fun bindPinPadSeed(seed: SourcedPinPadSeed): PinPadSeed

    @Multibinds
    abstract fun pinPadSeedSources(): Set<PinPadSeedSource>
}
