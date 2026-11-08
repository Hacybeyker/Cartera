package com.hacybeyker.cartera.feature.order.ui.form

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.domain.model.OrderFieldError
import com.hacybeyker.cartera.feature.order.domain.model.estimatedTotal

data class OrderFormUiState(
    val symbol: String,
    val side: OrderSide,
    val isLoading: Boolean = true,
    val error: AppError? = null,
    /** Solo las cuentas en las que se liquidan las acciones (USD). */
    val accounts: List<Account> = emptyList(),
    /** Acciones que ya se tienen del símbolo; en una compra no se piden y queda en 0. */
    val heldQuantity: Int = 0,
    val name: String? = null,
    /** Precio en vivo del canal; mientras no llega no se puede calcular el total ni continuar. */
    val price: Money? = null,
    val quantity: String = "",
    val accountId: String? = null,
    /** La persona eligió la cuenta en el sheet (no es la preseleccionada). */
    val isAccountChosen: Boolean = false,
    val isAccountSheetVisible: Boolean = false,
    val fieldErrors: Map<OrderField, OrderFieldError> = emptyMap(),
    val isExitConfirmationVisible: Boolean = false,
    val isExited: Boolean = false,
    /** Orden válida a confirmar; la UI navega y lo confirma con OrderOpened. */
    val order: MarketOrder? = null,
    val isReconnecting: Boolean = false,
    val contentVersion: Int = 0
) {
    val hasData: Boolean get() = quantity.isNotBlank() || isAccountChosen

    val selectedAccount: Account? get() = accounts.firstOrNull { it.id == accountId }

    /** Cantidad × precio en vivo; sin precio o sin una cantidad válida, no hay total que mostrar. */
    val estimatedTotal: Money?
        get() {
            val shares = quantity.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
            return price?.let { estimatedTotal(it, shares) }
        }

    val canContinue: Boolean get() = price != null

    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null }
}

sealed interface OrderFormAction {
    data class QuantityChanged(val value: String) : OrderFormAction

    data object AccountClicked : OrderFormAction

    data class AccountSelected(val accountId: String) : OrderFormAction

    data object AccountSheetDismissed : OrderFormAction

    data object ContinueClicked : OrderFormAction

    data object OrderOpened : OrderFormAction

    data object BackClicked : OrderFormAction

    data object ExitConfirmed : OrderFormAction

    data object ExitCancelled : OrderFormAction

    data object RetryClicked : OrderFormAction
}
