package com.hacybeyker.cartera.feature.biometric.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.CommonTestTags
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.feature.biometric.ui.offer.BiometricOfferAction
import com.hacybeyker.cartera.feature.biometric.ui.offer.BiometricOfferContent
import com.hacybeyker.cartera.feature.biometric.ui.offer.BiometricOfferTestTags
import com.hacybeyker.cartera.feature.biometric.ui.offer.BiometricOfferUiState
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityAction
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityContent
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityTestTags
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiometricContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the offer activates, skips and retries`() {
        val actions = mutableListOf<BiometricOfferAction>()
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                BiometricOfferContent(uiState = BiometricOfferUiState(dialogError = AppError.Network), onAction = {
                    actions += it
                })
            }
        }

        compose.onNodeWithTag(CommonTestTags.ERROR_DIALOG_RETRY).performClick()
        compose.onNodeWithTag(CommonTestTags.ERROR_DIALOG_CANCEL).performClick()
        compose.onNodeWithTag(BiometricOfferTestTags.ACTIVATE).performClick()
        compose.onNodeWithTag(BiometricOfferTestTags.SKIP).performClick()

        assertEquals(
            listOf(
                BiometricOfferAction.RetryClicked,
                BiometricOfferAction.ErrorDismissed,
                BiometricOfferAction.ActivateClicked,
                BiometricOfferAction.SkipClicked
            ),
            actions
        )
    }

    @Test
    fun `the security switch toggles the biometric login`() {
        val actions = mutableListOf<SecurityAction>()
        show(SecurityUiState(isLoading = false, isAvailable = true, isBiometricEnabled = true), actions)

        compose.onNodeWithTag(SecurityTestTags.BIOMETRIC).assertIsOn().performClick()

        assertEquals(listOf<SecurityAction>(SecurityAction.BiometricToggled(false)), actions)
    }

    @Test
    fun `without a fingerprint the switch is off and disabled`() {
        show(SecurityUiState(isLoading = false), mutableListOf())

        compose.onNodeWithTag(SecurityTestTags.BIOMETRIC).assertIsOff().assertIsNotEnabled()
    }

    private fun show(uiState: SecurityUiState, actions: MutableList<SecurityAction>) {
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                SecurityContent(uiState = uiState, onAction = { actions += it }, onBack = {})
            }
        }
    }
}
