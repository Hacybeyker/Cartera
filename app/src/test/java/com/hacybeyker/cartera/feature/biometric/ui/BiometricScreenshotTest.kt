package com.hacybeyker.cartera.feature.biometric.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.feature.biometric.ui.offer.BiometricOfferContent
import com.hacybeyker.cartera.feature.biometric.ui.offer.BiometricOfferUiState
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityChange
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityContent
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class BiometricScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val loaded = SecurityUiState(isLoading = false, isAvailable = true)

    @Test
    fun offer() = compose.captureLightAndDark("biometria_oferta") {
        BiometricOfferContent(uiState = BiometricOfferUiState(), onAction = {})
    }

    @Test
    fun offerUnavailable() = compose.captureLightAndDark("biometria_oferta_sin_huella") {
        BiometricOfferContent(uiState = BiometricOfferUiState(isUnavailable = true), onAction = {})
    }

    @Test
    fun offerError() = compose.captureLightAndDark("biometria_oferta_error_red", withWindows = true) {
        BiometricOfferContent(uiState = BiometricOfferUiState(dialogError = AppError.Network), onAction = {})
    }

    @Test
    fun securityEnabled() = compose.captureLightAndDark("seguridad_activado") {
        SecurityContent(uiState = loaded.copy(isBiometricEnabled = true), onAction = {}, onBack = {})
    }

    @Test
    fun securityDisabled() = compose.captureLightAndDark("seguridad_desactivado") {
        SecurityContent(uiState = loaded, onAction = {}, onBack = {})
    }

    @Test
    fun securityWithoutFingerprint() = compose.captureLightAndDark("seguridad_sin_huella") {
        SecurityContent(uiState = SecurityUiState(isLoading = false), onAction = {}, onBack = {})
    }

    @Test
    fun securityDisableError() = compose.captureLightAndDark("seguridad_error_desactivar", withWindows = true) {
        SecurityContent(
            uiState = loaded.copy(
                isBiometricEnabled = true,
                dialogError = AppError.Network,
                failedChange = SecurityChange.DISABLE
            ),
            onAction = {},
            onBack = {}
        )
    }
}
