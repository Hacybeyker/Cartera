package com.hacybeyker.cartera.feature.transfer.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.feature.transfer.ReferenceOrder
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferFieldError
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmContent
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmUiState
import com.hacybeyker.cartera.feature.transfer.ui.form.TransferFormContent
import com.hacybeyker.cartera.feature.transfer.ui.form.TransferFormUiState
import com.hacybeyker.cartera.feature.transfer.ui.result.TransferResultContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class TransferScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val filledForm = TransferFormUiState(
        isLoading = false,
        accounts = previewAccounts,
        sourceAccountId = "ACC-002",
        destination = "999-0000001-0-01",
        amount = "150.00"
    )
    private val confirm = TransferConfirmUiState(order = ReferenceOrder.copy(description = "Almuerzo"))

    @Test
    fun formFilled() = compose.captureLightAndDark("transferir_formulario") {
        TransferFormContent(uiState = filledForm, onAction = {})
    }

    @Test
    fun formErrors() = compose.captureLightAndDark("transferir_errores") {
        TransferFormContent(
            uiState = filledForm.copy(
                sourceAccountId = null,
                destination = "123",
                amount = "0",
                fieldErrors = mapOf(
                    TransferField.SOURCE to TransferFieldError.REQUIRED,
                    TransferField.DESTINATION to TransferFieldError.INVALID_FORMAT,
                    TransferField.AMOUNT to TransferFieldError.MUST_BE_POSITIVE
                )
            ),
            onAction = {}
        )
    }

    @Test
    fun formLoading() = compose.captureLightAndDark("transferir_cargando") {
        TransferFormContent(uiState = TransferFormUiState(), onAction = {})
    }

    @Test
    fun formExitConfirmation() = compose.captureLightAndDark("transferir_salir", withWindows = true) {
        TransferFormContent(uiState = filledForm.copy(isExitConfirmationVisible = true), onAction = {})
    }

    @Test
    fun confirmation() = compose.captureLightAndDark("transferir_confirmacion") {
        TransferConfirmContent(uiState = confirm, onAction = {}, onBack = {})
    }

    @Test
    fun confirmationSubmitting() = compose.captureLightAndDark("transferir_confirmando", withWindows = true) {
        TransferConfirmContent(uiState = confirm.copy(isSubmitting = true), onAction = {}, onBack = {})
    }

    @Test
    fun confirmationNetworkError() = compose.captureLightAndDark("transferir_error_red", withWindows = true) {
        TransferConfirmContent(uiState = confirm.copy(dialogError = AppError.Network), onAction = {}, onBack = {})
    }

    @Test
    fun insufficientFunds() = compose.captureLightAndDark("transferir_fondos_insuficientes", withWindows = true) {
        TransferConfirmContent(uiState = confirm.copy(rejection = AppError.InsufficientFunds), onAction = {
        }, onBack = {})
    }

    @Test
    fun destinationNotFound() = compose.captureLightAndDark("transferir_destino_inexistente", withWindows = true) {
        TransferConfirmContent(uiState = confirm.copy(rejection = AppError.DestinationNotFound), onAction = {
        }, onBack = {})
    }

    @Test
    fun currencyMismatch() = compose.captureLightAndDark("transferir_moneda_distinta", withWindows = true) {
        TransferConfirmContent(uiState = confirm.copy(rejection = AppError.CurrencyMismatch), onAction = {
        }, onBack = {})
    }

    @Test
    fun confirmationFieldErrors() = compose.captureLightAndDark("transferir_confirmacion_errores") {
        TransferConfirmContent(
            uiState = confirm.copy(fieldErrors = mapOf(TransferField.SOURCE to TransferFieldError.NOT_OWNED)),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun cancelConfirmation() = compose.captureLightAndDark("transferir_cancelar", withWindows = true) {
        TransferConfirmContent(uiState = confirm.copy(isCancelConfirmationVisible = true), onAction = {}, onBack = {})
    }

    @Test
    fun resultSuccess() = compose.captureLightAndDark("transferir_resultado_exito") {
        TransferResultContent(receipt = TransferReceiptArgs("90000001", "2150.00", "PEN"), onBackToHome = {})
    }

    @Test
    fun resultFailure() = compose.captureLightAndDark("transferir_resultado_error") {
        TransferResultContent(receipt = null, onBackToHome = {})
    }
}
