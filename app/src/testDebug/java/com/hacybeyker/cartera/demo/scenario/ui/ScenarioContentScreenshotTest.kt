package com.hacybeyker.cartera.demo.scenario.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.demo.backend.FailureMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ScenarioContentScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun defaults() = compose.captureLightAndDark("escenario_valores_defecto") {
        ScenarioContent(
            uiState = ScenarioUiState(
                latency = "default=400",
                failureRate = "default=0",
                sessionTtl = "900",
                tickMs = "2000",
                streamDrop = "0",
                quoteTtl = "30",
                pinPadSeed = "0"
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun rejectedValues() = compose.captureLightAndDark("escenario_valores_invalidos") {
        ScenarioContent(
            uiState = ScenarioUiState(
                latency = "default=400,movements=2000",
                failureRate = "default=0,transfers=100",
                failureMode = FailureMode.TIMEOUT,
                sessionTtl = "60",
                backendDown = true,
                tickMs = "0",
                streamDrop = "15",
                quoteTtl = "5",
                pinPadSeed = "7",
                flags = setOf(FeatureFlag.MOVEMENTS_FULL_PAGE),
                message = ScenarioMessage.Rejected(listOf("session_ttl=0"))
            ),
            onAction = {},
            onBack = {}
        )
    }

    // Más alto que el Pixel 5 para que entren todos los interruptores sin desplazar.
    @Test
    @Config(qualifiers = "+h1400dp")
    fun clientFlags() = compose.captureLightAndDark("escenario_interruptores") {
        ScenarioContent(
            uiState = ScenarioUiState(
                latency = "default=400",
                failureRate = "default=0",
                sessionTtl = "900",
                tickMs = "250",
                streamDrop = "0",
                quoteTtl = "30",
                pinPadSeed = "7",
                flags = setOf(FeatureFlag.QUOTES_FULL_REFRESH, FeatureFlag.CHART_LIVE_PATH)
            ),
            onAction = {},
            onBack = {}
        )
    }
}
