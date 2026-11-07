package com.hacybeyker.cartera.feature.terms.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
class TermsContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val actions = mutableListOf<TermsAction>()

    @Test
    fun `the checkbox is reachable by its text and accept waits for it`() {
        show(TermsUiState())

        compose.onNodeWithText("Aceptar").assertIsNotEnabled()
        compose.onNodeWithText("He leído y acepto", substring = true).performClick()
        compose.onNodeWithText("Rechazar").performClick()

        assertEquals(listOf(TermsAction.CheckedChanged(true), TermsAction.RejectClicked), actions)
    }

    @Test
    fun `checked terms can be accepted`() {
        show(TermsUiState(isChecked = true))

        compose.onNodeWithText("Aceptar").assertIsEnabled().performClick()

        assertEquals(listOf<TermsAction>(TermsAction.AcceptClicked), actions)
    }

    private fun show(uiState: TermsUiState) {
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                TermsContent(uiState = uiState, onAction = { actions += it })
            }
        }
    }
}
