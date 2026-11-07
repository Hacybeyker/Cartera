package com.hacybeyker.cartera.feature.onboarding.ui

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
class OnboardingContentScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun firstPage() = compose.captureLightAndDark("onboarding_primera_pagina") {
        OnboardingContent(onAction = {})
    }

    @Test
    fun lastPage() = compose.captureLightAndDark("onboarding_ultima_pagina") {
        OnboardingContent(onAction = {}, initialPage = 2)
    }
}
