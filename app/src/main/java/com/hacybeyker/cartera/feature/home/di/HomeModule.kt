package com.hacybeyker.cartera.feature.home.di

import com.hacybeyker.cartera.feature.home.data.repository.RemoteProfileRepository
import com.hacybeyker.cartera.feature.home.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    abstract fun bindProfileRepository(repository: RemoteProfileRepository): ProfileRepository
}
