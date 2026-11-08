package com.hacybeyker.cartera.feature.exchange.di

import com.hacybeyker.cartera.feature.exchange.data.repository.RemoteExchangeRepository
import com.hacybeyker.cartera.feature.exchange.domain.repository.ExchangeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ExchangeModule {

    @Binds
    abstract fun bindExchangeRepository(repository: RemoteExchangeRepository): ExchangeRepository
}
