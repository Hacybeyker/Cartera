package com.hacybeyker.cartera.core.ui.marks.di

import com.hacybeyker.cartera.core.ui.marks.VisibleContentMarker
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class VisibleContentModule {

    @Multibinds
    abstract fun visibleContentMarkers(): Set<VisibleContentMarker>
}
