package com.hacybeyker.cartera.feature.order.di

import com.hacybeyker.cartera.feature.order.data.repository.RemoteOrderRepository
import com.hacybeyker.cartera.feature.order.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class OrderModule {

    @Binds
    abstract fun bindOrderRepository(repository: RemoteOrderRepository): OrderRepository
}
