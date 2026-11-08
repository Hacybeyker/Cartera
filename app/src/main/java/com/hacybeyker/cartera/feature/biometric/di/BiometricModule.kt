package com.hacybeyker.cartera.feature.biometric.di

import com.hacybeyker.cartera.feature.biometric.data.repository.RemoteBiometricEnrollmentRepository
import com.hacybeyker.cartera.feature.biometric.domain.repository.BiometricEnrollmentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BiometricModule {

    @Binds
    abstract fun bindBiometricEnrollmentRepository(
        repository: RemoteBiometricEnrollmentRepository
    ): BiometricEnrollmentRepository
}
