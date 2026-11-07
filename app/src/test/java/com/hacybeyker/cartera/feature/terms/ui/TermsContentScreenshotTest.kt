package com.hacybeyker.cartera.feature.terms.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class TermsContentScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun unchecked() = compose.captureLightAndDark("terminos_sin_marcar") {
        TermsContent(uiState = TermsUiState(), onAction = {})
    }

    @Test
    fun checked() = compose.captureLightAndDark("terminos_marcado") {
        TermsContent(uiState = TermsUiState(isChecked = true), onAction = {})
    }
}
