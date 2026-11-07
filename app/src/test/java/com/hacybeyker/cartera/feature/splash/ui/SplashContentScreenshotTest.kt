package com.hacybeyker.cartera.feature.splash.ui

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
class SplashContentScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loading() = compose.captureLightAndDark("splash_cargando") {
        SplashContent(uiState = SplashUiState.Loading, onAction = {})
    }

    @Test
    fun error() = compose.captureLightAndDark("splash_error") {
        SplashContent(uiState = SplashUiState.Error(AppError.Server), onAction = {})
    }
}
