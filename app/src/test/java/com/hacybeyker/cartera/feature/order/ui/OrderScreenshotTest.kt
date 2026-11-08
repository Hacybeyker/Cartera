package com.hacybeyker.cartera.feature.order.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.feature.order.DollarAccount
import com.hacybeyker.cartera.feature.order.ReferenceBuy
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.domain.model.OrderFieldError
import com.hacybeyker.cartera.feature.order.ui.confirm.OrderConfirmContent
import com.hacybeyker.cartera.feature.order.ui.confirm.OrderConfirmUiState
import com.hacybeyker.cartera.feature.order.ui.form.OrderFormContent
import com.hacybeyker.cartera.feature.order.ui.form.OrderFormUiState
import com.hacybeyker.cartera.feature.order.ui.result.OrderResultContent
import com.hacybeyker.cartera.feature.order.usd
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class OrderScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val emptyForm = OrderFormUiState(
        symbol = "ANDM",
        side = OrderSide.BUY,
        isLoading = false,
        accounts = listOf(DollarAccount),
        name = "Andina Minerals",
        price = usd("42.10"),
        accountId = "ACC-003"
    )
    private val confirm = OrderConfirmUiState(order = ReferenceBuy)

    private val buyReceipt = OrderReceiptArgs(
        side = OrderSide.BUY,
        symbol = "ANDM",
        operationNumber = "70000001",
        priceValue = "42.10",
        totalValue = "84.20",
        balanceValue = "966.00",
        currency = "USD",
        holdingQuantity = 12,
        holdingAverageValue = "40.35"
    )

    @Test
    fun formEmpty() = compose.captureLightAndDark("orden_formulario_vacio") {
        OrderFormContent(uiState = emptyForm, onAction = {})
    }

    @Test
    fun formFilled() = compose.captureLightAndDark("orden_formulario") {
        OrderFormContent(uiState = emptyForm.copy(quantity = "2"), onAction = {})
    }

    @Test
    fun formErrors() = compose.captureLightAndDark("orden_errores") {
        OrderFormContent(
            uiState = emptyForm.copy(
                quantity = "25",
                accountId = null,
                fieldErrors = mapOf(
                    OrderField.QUANTITY to OrderFieldError.EXCEEDS_BALANCE,
                    OrderField.ACCOUNT to OrderFieldError.REQUIRED
                )
            ),
            onAction = {}
        )
    }

    @Test
    fun sellFormErrors() = compose.captureLightAndDark("orden_venta_errores") {
        OrderFormContent(
            uiState = emptyForm.copy(
                symbol = "QORI",
                side = OrderSide.SELL,
                name = "Qori Gold",
                price = usd("115.20"),
                heldQuantity = 2,
                quantity = "3",
                fieldErrors = mapOf(OrderField.QUANTITY to OrderFieldError.EXCEEDS_HOLDINGS)
            ),
            onAction = {}
        )
    }

    @Test
    fun formWaitingForPrice() = compose.captureLightAndDark("orden_sin_precio") {
        OrderFormContent(
            uiState = emptyForm.copy(name = null, price = null, quantity = "2", isReconnecting = true),
            onAction = {}
        )
    }

    @Test
    fun formLoading() = compose.captureLightAndDark("orden_cargando") {
        OrderFormContent(uiState = OrderFormUiState(symbol = "ANDM", side = OrderSide.BUY), onAction = {})
    }

    @Test
    fun formError() = compose.captureLightAndDark("orden_error") {
        OrderFormContent(uiState = emptyForm.copy(error = AppError.Network), onAction = {})
    }

    @Test
    fun formExitConfirmation() = compose.captureLightAndDark("orden_salir", withWindows = true) {
        OrderFormContent(uiState = emptyForm.copy(quantity = "2", isExitConfirmationVisible = true), onAction = {})
    }

    @Test
    fun formAccountsSheet() = compose.captureLightAndDark("orden_sheet_cuentas", withWindows = true) {
        OrderFormContent(uiState = emptyForm.copy(isAccountSheetVisible = true), onAction = {})
    }

    @Test
    fun confirmation() = compose.captureLightAndDark("orden_confirmacion") {
        OrderConfirmContent(uiState = confirm, onAction = {}, onBack = {})
    }

    @Test
    fun confirmationSubmitting() = compose.captureLightAndDark("orden_confirmando", withWindows = true) {
        OrderConfirmContent(uiState = confirm.copy(isSubmitting = true), onAction = {}, onBack = {})
    }

    @Test
    fun confirmationNetworkError() = compose.captureLightAndDark("orden_error_red", withWindows = true) {
        OrderConfirmContent(uiState = confirm.copy(dialogError = AppError.Network), onAction = {}, onBack = {})
    }

    @Test
    fun insufficientFunds() = compose.captureLightAndDark("orden_fondos_insuficientes", withWindows = true) {
        OrderConfirmContent(uiState = confirm.copy(rejection = AppError.InsufficientFunds), onAction = {}, onBack = {})
    }

    @Test
    fun insufficientHoldings() = compose.captureLightAndDark("orden_tenencia_insuficiente", withWindows = true) {
        OrderConfirmContent(
            uiState = OrderConfirmUiState(order = ReferenceBuy.copy(side = OrderSide.SELL, quantity = 3))
                .copy(rejection = AppError.InsufficientHoldings),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun confirmationFieldErrors() = compose.captureLightAndDark("orden_confirmacion_errores") {
        OrderConfirmContent(
            uiState = confirm.copy(
                fieldErrors = mapOf(
                    OrderField.QUANTITY to OrderFieldError.MUST_BE_POSITIVE,
                    OrderField.ACCOUNT to OrderFieldError.UNSUPPORTED_CURRENCY
                )
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun cancelConfirmation() = compose.captureLightAndDark("orden_cancelar", withWindows = true) {
        OrderConfirmContent(uiState = confirm.copy(isCancelConfirmationVisible = true), onAction = {}, onBack = {})
    }

    @Test
    fun resultBuy() = compose.captureLightAndDark("orden_resultado_compra") {
        OrderResultContent(receipt = buyReceipt, onBackToMarket = {})
    }

    @Test
    fun resultSellAll() = compose.captureLightAndDark("orden_resultado_venta_total") {
        OrderResultContent(
            receipt = buyReceipt.copy(
                side = OrderSide.SELL,
                symbol = "QORI",
                priceValue = "115.20",
                totalValue = "230.40",
                balanceValue = "1196.40",
                holdingQuantity = 0,
                holdingAverageValue = null
            ),
            onBackToMarket = {}
        )
    }

    @Test
    fun resultFailure() = compose.captureLightAndDark("orden_resultado_error") {
        OrderResultContent(receipt = null, onBackToMarket = {})
    }
}
