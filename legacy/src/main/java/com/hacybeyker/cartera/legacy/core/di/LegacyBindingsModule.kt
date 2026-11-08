package com.hacybeyker.cartera.legacy.core.di

import com.hacybeyker.cartera.legacy.account.data.RemoteAccountRepository
import com.hacybeyker.cartera.legacy.account.domain.AccountRepository
import com.hacybeyker.cartera.legacy.billers.data.RemoteBillerRepository
import com.hacybeyker.cartera.legacy.billers.domain.BillerRepository
import com.hacybeyker.cartera.legacy.core.data.RxSchedulerProvider
import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import com.hacybeyker.cartera.legacy.debts.data.RemoteDebtRepository
import com.hacybeyker.cartera.legacy.debts.domain.DebtRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LegacyBindingsModule {

    @Binds
    abstract fun bindBillerRepository(repository: RemoteBillerRepository): BillerRepository

    @Binds
    abstract fun bindDebtRepository(repository: RemoteDebtRepository): DebtRepository

    @Binds
    abstract fun bindAccountRepository(repository: RemoteAccountRepository): AccountRepository

    @Binds
    abstract fun bindSchedulerProvider(provider: RxSchedulerProvider): SchedulerProvider
}
