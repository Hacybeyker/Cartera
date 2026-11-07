package com.hacybeyker.cartera.feature.movements.di

import com.hacybeyker.cartera.feature.movements.data.repository.RemoteMovementDetailRepository
import com.hacybeyker.cartera.feature.movements.domain.repository.MovementDetailRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MovementsModule {

    @Binds
    abstract fun bindMovementDetailRepository(repository: RemoteMovementDetailRepository): MovementDetailRepository
}
