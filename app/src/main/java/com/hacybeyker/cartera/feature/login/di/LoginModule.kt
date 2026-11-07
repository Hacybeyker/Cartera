package com.hacybeyker.cartera.feature.login.di

import com.hacybeyker.cartera.feature.login.data.repository.RemoteAuthRepository
import com.hacybeyker.cartera.feature.login.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LoginModule {

    @Binds
    abstract fun bindAuthRepository(repository: RemoteAuthRepository): AuthRepository
}
