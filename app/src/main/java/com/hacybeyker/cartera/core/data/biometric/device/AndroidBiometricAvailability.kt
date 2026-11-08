package com.hacybeyker.cartera.core.data.biometric.device

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import com.hacybeyker.cartera.core.domain.biometric.BiometricAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidBiometricAvailability @Inject constructor(@ApplicationContext private val context: Context) :
    BiometricAvailability {

    override fun isAvailable(): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
}
