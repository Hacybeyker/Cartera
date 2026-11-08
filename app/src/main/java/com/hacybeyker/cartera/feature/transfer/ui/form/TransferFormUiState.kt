package com.hacybeyker.cartera.feature.transfer.ui.form

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferFieldError
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder

data class TransferFormUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val accounts: List<Account> = emptyList(),
    val sourceAccountId: String? = null,
    val destination: String = "",
    val amount: String = "",
    val description: String = "",
    val fieldErrors: Map<TransferField, TransferFieldError> = emptyMap(),
    val isExitConfirmationVisible: Boolean = false,
    val isExited: Boolean = false,
    /** Orden válida a confirmar; la UI navega y lo confirma con OrderOpened. */
    val order: TransferOrder? = null,
    val contentVersion: Int = 0
) {
    val hasData: Boolean
        get() = sourceAccountId != null || destination.isNotBlank() || amount.isNotBlank() || description.isNotBlank()

    val currency: String? get() = accounts.firstOrNull { it.id == sourceAccountId }?.currency

    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null }
}

sealed interface TransferFormAction {
    data class SourceSelected(val accountId: String) : TransferFormAction

    data class DestinationChanged(val value: String) : TransferFormAction

    data class AmountChanged(val value: String) : TransferFormAction

    data class DescriptionChanged(val value: String) : TransferFormAction

    data object ContinueClicked : TransferFormAction

    data object OrderOpened : TransferFormAction

    data object BackClicked : TransferFormAction

    data object ExitConfirmed : TransferFormAction

    data object ExitCancelled : TransferFormAction

    data object RetryClicked : TransferFormAction
}
