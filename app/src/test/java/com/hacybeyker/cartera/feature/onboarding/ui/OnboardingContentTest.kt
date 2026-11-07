package com.hacybeyker.cartera.feature.onboarding.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val actions = mutableListOf<OnboardingAction>()

    @Test
    fun `next walks the three pages until start`() {
        show()

        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Cada movimiento, al detalle").assertExists()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Siguiente").assertDoesNotExist()
        compose.onNodeWithText("Empezar").performClick()

        assertEquals(listOf<OnboardingAction>(OnboardingAction.StartClicked), actions)
    }

    @Test
    fun `skip is available on the first page`() {
        show()

        compose.onNodeWithText("Omitir").performClick()

        assertEquals(listOf<OnboardingAction>(OnboardingAction.SkipClicked), actions)
    }

    private fun show() {
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                OnboardingContent(onAction = { actions += it })
            }
        }
    }
}
