package com.hacybeyker.cartera.feature.home.ui.exchange

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.core.domain.exchange.parseExchangeAmount
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeDraft

/** Qué cuenta se está eligiendo en el sheet. */
enum class ExchangeAccountRole {
    SOURCE,
    DESTINATION
}

data class ExchangeUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val accounts: List<Account> = emptyList(),
    /** La última cotización: la de la carga y después las del canal, siempre la más reciente. */
    val quote: FxQuote? = null,
    val direction: ExchangeDirection = ExchangeDirection.USD_TO_PEN,
    /** En la moneda de origen. */
    val amount: String = "",
    val sourceAccountId: String? = null,
    val destinationAccountId: String? = null,
    val accountSheetFor: ExchangeAccountRole? = null,
    val isHowWeCalculateVisible: Boolean = false,
    val fieldErrors: Map<ExchangeField, ExchangeFieldError> = emptyMap(),
    /** Continuar está pidiendo la cotización con que se confirmará. */
    val isPreparing: Boolean = false,
    /** No se pudo pedir esa cotización: modal con Reintentar y Cancelar. */
    val dialogError: AppError? = null,
    /** Cambio válido a confirmar; la UI navega y lo confirma con OrderOpened. */
    val order: ExchangeOrder? = null,
    val isReconnecting: Boolean = false,
    /** Sube con cada carga o refresco correcto (no con las cotizaciones del canal): clave de la marca `cambio`. */
    val contentVersion: Int = 0
) {
    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null }

    val sourceAccount: Account? get() = accounts.firstOrNull { it.id == sourceAccountId }

    val destinationAccount: Account? get() = accounts.firstOrNull { it.id == destinationAccountId }

    /** Las cuentas que ofrece el sheet: solo las de la moneda que pide el sentido para ese lado. */
    fun accountsFor(role: ExchangeAccountRole): List<Account> {
        val currency = when (role) {
            ExchangeAccountRole.SOURCE -> direction.sourceCurrency
            ExchangeAccountRole.DESTINATION -> direction.destinationCurrency
        }
        return accounts.filter { it.currency == currency }
    }

    /** Lo que se recibiría con la cotización en vivo; sin un monto válido, nada. */
    val estimatedCredit: Money?
        get() {
            val value = parseExchangeAmount(amount)?.takeIf { it.signum() > 0 } ?: return null
            val current = quote ?: return null
            return Money(direction.credit(value, direction.rate(current)), direction.destinationCurrency)
        }

    val draft: ExchangeDraft
        get() = ExchangeDraft(
            direction = direction,
            amount = amount,
            sourceAccountId = sourceAccountId,
            destinationAccountId = destinationAccountId
        )

    /** Invertir el sentido intercambia las cuentas: la de origen pasa a ser la de destino y al revés. */
    fun reversed(): ExchangeUiState = copy(
        direction = direction.reversed,
        sourceAccountId = destinationAccountId,
        destinationAccountId = sourceAccountId,
        fieldErrors = emptyMap()
    )

    fun withAccount(accountId: String): ExchangeUiState = when (accountSheetFor) {
        // Otro origen cambia el saldo con que se valida el monto.
        ExchangeAccountRole.SOURCE -> copy(
            sourceAccountId = accountId,
            accountSheetFor = null,
            fieldErrors = fieldErrors - ExchangeField.SOURCE - ExchangeField.AMOUNT
        )
        ExchangeAccountRole.DESTINATION -> copy(
            destinationAccountId = accountId,
            accountSheetFor = null,
            fieldErrors = fieldErrors - ExchangeField.DESTINATION
        )
        null -> this
    }

    /** Con una sola cuenta posible para un lado, ya queda elegida. */
    fun withDefaultAccounts(): ExchangeUiState = copy(
        sourceAccountId = sourceAccountId ?: accountsFor(ExchangeAccountRole.SOURCE).singleOrNull()?.id,
        destinationAccountId = destinationAccountId
            ?: accountsFor(ExchangeAccountRole.DESTINATION).singleOrNull()?.id
    )

    /** Se queda con [incoming] salvo que sea más vieja que la que ya hay. */
    fun withQuote(incoming: FxQuote): ExchangeUiState {
        val current = quote
        return if (current == null || !incoming.ts.isBefore(current.ts)) copy(quote = incoming) else this
    }
}

sealed interface ExchangeAction {
    data class AmountChanged(val value: String) : ExchangeAction

    data object DirectionToggled : ExchangeAction

    data class AccountClicked(val role: ExchangeAccountRole) : ExchangeAction

    data class AccountSelected(val accountId: String) : ExchangeAction

    data object HowWeCalculateClicked : ExchangeAction

    /** Se cerró cualquiera de los dos sheets sin elegir. */
    data object SheetDismissed : ExchangeAction

    data object ContinueClicked : ExchangeAction

    data object OrderOpened : ExchangeAction

    /** Reintentar la carga del tab. */
    data object RetryClicked : ExchangeAction

    /** Cancelar en el modal de error de Continuar. */
    data object ErrorDismissed : ExchangeAction

    /** El tab volvió a verse: si un cambio o una transferencia movieron saldos, se vuelven a pedir las cuentas. */
    data object Shown : ExchangeAction
}
