package com.hacybeyker.cartera.core.data.biometric.di

import android.content.Context
import android.content.SharedPreferences
import com.hacybeyker.cartera.core.data.biometric.BiometricCiphers
import com.hacybeyker.cartera.core.data.biometric.BiometricPrompter
import com.hacybeyker.cartera.core.data.biometric.BiometricStorage
import com.hacybeyker.cartera.core.data.biometric.KeystoreDeviceTokenVault
import com.hacybeyker.cartera.core.data.biometric.device.ActivityBiometricPrompter
import com.hacybeyker.cartera.core.data.biometric.device.AndroidBiometricAvailability
import com.hacybeyker.cartera.core.data.biometric.device.AndroidKeystoreCiphers
import com.hacybeyker.cartera.core.domain.biometric.BiometricAvailability
import com.hacybeyker.cartera.core.domain.biometric.DeviceTokenVault
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BiometricDataModule {

    @Binds
    abstract fun bindDeviceTokenVault(vault: KeystoreDeviceTokenVault): DeviceTokenVault

    @Binds
    abstract fun bindBiometricAvailability(availability: AndroidBiometricAvailability): BiometricAvailability

    @Binds
    abstract fun bindBiometricCiphers(ciphers: AndroidKeystoreCiphers): BiometricCiphers

    @Binds
    abstract fun bindBiometricPrompter(prompter: ActivityBiometricPrompter): BiometricPrompter

    companion object {
        @Provides
        @BiometricStorage
        fun provideBiometricPreferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences(KeystoreDeviceTokenVault.FILE_NAME, Context.MODE_PRIVATE)
    }
}
