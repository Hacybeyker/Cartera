package com.hacybeyker.cartera.feature.exchange.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmContent
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmFailure
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmStep
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmUiState
import com.hacybeyker.cartera.feature.exchange.ui.confirm.previewExchangeOrder
import com.hacybeyker.cartera.feature.exchange.ui.confirm.previewNewQuote
import com.hacybeyker.cartera.feature.exchange.ui.result.FxResultContent
import com.hacybeyker.cartera.feature.exchange.ui.result.previewExchangeReceipt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class FxScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val confirm = FxConfirmUiState(order = previewExchangeOrder, remainingSeconds = 25)

    @Test
    fun confirmation() = compose.captureLightAndDark("cambio_confirmacion") {
        FxConfirmContent(uiState = confirm, onAction = {}, onBack = {})
    }

    @Test
    fun confirmationExpired() = compose.captureLightAndDark("cambio_confirmacion_vencida") {
        FxConfirmContent(uiState = confirm.copy(remainingSeconds = 0), onAction = {}, onBack = {})
    }

    @Test
    fun confirmationFieldErrors() = compose.captureLightAndDark("cambio_confirmacion_errores") {
        FxConfirmContent(
            uiState = confirm.copy(
                fieldErrors = mapOf(
                    ExchangeField.AMOUNT to ExchangeFieldError.TOO_SMALL,
                    ExchangeField.SOURCE to ExchangeFieldError.NOT_OWNED,
                    ExchangeField.QUOTE to ExchangeFieldError.NOT_FOUND
                )
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun submitting() = compose.captureLightAndDark("cambio_confirmando", withWindows = true) {
        FxConfirmContent(uiState = confirm.copy(isSubmitting = true), onAction = {}, onBack = {})
    }

    @Test
    fun quoteExpired() = compose.captureLightAndDark("cambio_cotizacion_vencida", withWindows = true) {
        FxConfirmContent(
            uiState = confirm.copy(remainingSeconds = 0, newQuote = previewNewQuote),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun networkError() = compose.captureLightAndDark("cambio_error_red", withWindows = true) {
        FxConfirmContent(
            uiState = confirm.copy(failure = FxConfirmFailure(AppError.Network, FxConfirmStep.CONFIRM)),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun renewError() = compose.captureLightAndDark("cambio_error_renovar", withWindows = true) {
        FxConfirmContent(
            uiState = confirm.copy(
                remainingSeconds = 0,
                failure = FxConfirmFailure(AppError.Server, FxConfirmStep.RENEW_QUOTE)
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun insufficientFunds() = compose.captureLightAndDark("cambio_fondos_insuficientes", withWindows = true) {
        FxConfirmContent(uiState = confirm.copy(rejection = AppError.InsufficientFunds), onAction = {}, onBack = {})
    }

    @Test
    fun currencyMismatch() = compose.captureLightAndDark("cambio_moneda_distinta", withWindows = true) {
        FxConfirmContent(uiState = confirm.copy(rejection = AppError.CurrencyMismatch), onAction = {}, onBack = {})
    }

    @Test
    fun cancelConfirmation() = compose.captureLightAndDark("cambio_cancelar", withWindows = true) {
        FxConfirmContent(uiState = confirm.copy(isCancelConfirmationVisible = true), onAction = {}, onBack = {})
    }

    @Test
    fun result() = compose.captureLightAndDark("cambio_resultado") {
        FxResultContent(receipt = previewExchangeReceipt, onBackToExchange = {})
    }

    @Test
    fun resultFailure() = compose.captureLightAndDark("cambio_resultado_error") {
        FxResultContent(receipt = null, onBackToExchange = {})
    }
}
