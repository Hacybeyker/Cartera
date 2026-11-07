package com.hacybeyker.cartera.navigation.di

import com.hacybeyker.cartera.navigation.NavEntryInstaller
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {

    @Multibinds
    abstract fun navEntryInstallers(): Set<NavEntryInstaller>
}
