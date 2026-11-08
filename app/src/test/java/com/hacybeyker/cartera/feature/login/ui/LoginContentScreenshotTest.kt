package com.hacybeyker.cartera.feature.login.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class LoginContentScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val filled = LoginUiState(username = "ana.torres", password = "Cartera2026")

    @Test
    fun empty() = compose.captureLightAndDark("login_vacio") {
        LoginContent(uiState = LoginUiState(), onAction = {})
    }

    @Test
    fun passwordVisible() = compose.captureLightAndDark("login_contrasena_visible") {
        LoginContent(uiState = filled.copy(isPasswordVisible = true), onAction = {})
    }

    @Test
    fun invalidCredentials() = compose.captureLightAndDark("login_credenciales_invalidas") {
        LoginContent(uiState = filled.copy(formError = LoginFormError.INVALID_CREDENTIALS), onAction = {})
    }

    @Test
    fun biometric() = compose.captureLightAndDark("login_huella") {
        LoginContent(uiState = LoginUiState(isBiometricChecked = true, isBiometricVisible = true), onAction = {})
    }

    @Test
    fun deviceRevoked() = compose.captureLightAndDark("login_dispositivo_invalido") {
        LoginContent(
            uiState = LoginUiState(isBiometricChecked = true, formError = LoginFormError.DEVICE_REVOKED),
            onAction = {}
        )
    }

    @Test
    fun biometricChanged() = compose.captureLightAndDark("login_huella_cambiada") {
        LoginContent(
            uiState = LoginUiState(isBiometricChecked = true, formError = LoginFormError.BIOMETRIC_CHANGED),
            onAction = {}
        )
    }

    @Test
    fun submitting() = compose.captureLightAndDark("login_progreso", withWindows = true) {
        LoginContent(uiState = filled.copy(isSubmitting = true), onAction = {})
    }

    @Test
    fun networkError() = compose.captureLightAndDark("login_error_red", withWindows = true) {
        LoginContent(uiState = filled.copy(dialogError = AppError.Network), onAction = {})
    }
}
