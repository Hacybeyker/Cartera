package com.hacybeyker.cartera.feature.login.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.CommonTestTags
import com.hacybeyker.cartera.core.domain.AppError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val actions = mutableListOf<LoginAction>()

    @Test
    fun `typing enables submit and toggling the password emits its action`() {
        show(LoginUiState())

        compose.onNodeWithTag(LoginTestTags.USERNAME).performTextInput("ana.torres")
        compose.onNodeWithTag(LoginTestTags.PASSWORD).performTextInput("Cartera2026")
        compose.onNodeWithTag(LoginTestTags.SHOW_PASSWORD).performClick()

        assertEquals(
            listOf(
                LoginAction.UsernameChanged("ana.torres"),
                LoginAction.PasswordChanged("Cartera2026"),
                LoginAction.PasswordVisibilityToggled
            ),
            actions
        )
        compose.onNodeWithTag(LoginTestTags.SUBMIT).assertIsEnabled()
    }

    @Test
    fun `submit stays disabled while a field is empty`() {
        show(LoginUiState(username = "ana.torres"))

        compose.onNodeWithTag(LoginTestTags.SUBMIT).assertIsNotEnabled()
    }

    @Test
    fun `filled form submits and shows the credentials error`() {
        show(LoginUiState("ana.torres", "x", formError = LoginFormError.INVALID_CREDENTIALS))

        compose.onNodeWithTag(LoginTestTags.ERROR).assertExists()
        compose.onNodeWithTag(LoginTestTags.SUBMIT).assertIsEnabled().performClick()

        assertEquals(listOf<LoginAction>(LoginAction.SubmitClicked), actions)
    }

    @Test
    fun `network error modal retries and cancels`() {
        show(LoginUiState("ana.torres", "x", dialogError = AppError.Network))

        compose.onNodeWithTag(CommonTestTags.ERROR_DIALOG_RETRY).performClick()
        compose.onNodeWithTag(CommonTestTags.ERROR_DIALOG_CANCEL).performClick()

        assertEquals(listOf(LoginAction.RetryClicked, LoginAction.ErrorDismissed), actions)
    }

    // Los campos son controlados: el host aplica lo escrito como lo haría el ViewModel.
    private fun show(initial: LoginUiState) {
        compose.setContent {
            var uiState by remember { mutableStateOf(initial) }
            CarteraTheme(dynamicColor = false) {
                LoginContent(
                    uiState = uiState,
                    onAction = { action ->
                        actions += action
                        uiState = when (action) {
                            is LoginAction.UsernameChanged -> uiState.copy(username = action.value)
                            is LoginAction.PasswordChanged -> uiState.copy(password = action.value)
                            else -> uiState
                        }
                    }
                )
            }
        }
    }
}
