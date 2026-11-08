package com.hacybeyker.cartera.core.data.flags.di

import com.hacybeyker.cartera.core.data.flags.BuildConfigFeatureFlags
import com.hacybeyker.cartera.core.data.flags.FeatureFlagSource
import com.hacybeyker.cartera.core.domain.flags.FeatureFlags
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureFlagsModule {

    @Binds
    abstract fun bindFeatureFlags(flags: BuildConfigFeatureFlags): FeatureFlags

    @Multibinds
    abstract fun featureFlagSources(): Set<FeatureFlagSource>
}
