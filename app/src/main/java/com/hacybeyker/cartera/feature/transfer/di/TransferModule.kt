package com.hacybeyker.cartera.feature.transfer.di

import com.hacybeyker.cartera.feature.transfer.data.repository.RemoteTransferRepository
import com.hacybeyker.cartera.feature.transfer.data.repository.UuidIdempotencyKeyFactory
import com.hacybeyker.cartera.feature.transfer.domain.repository.IdempotencyKeyFactory
import com.hacybeyker.cartera.feature.transfer.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TransferModule {

    @Binds
    abstract fun bindTransferRepository(repository: RemoteTransferRepository): TransferRepository

    @Binds
    abstract fun bindIdempotencyKeyFactory(factory: UuidIdempotencyKeyFactory): IdempotencyKeyFactory
}
