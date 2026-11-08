package com.hacybeyker.cartera.feature.home.domain.model

import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.core.domain.market.FxQuote

/** Lo que el tab Cambio necesita al abrirse: las cuentas propias y la cotización vigente. */
data class ExchangeOverview(val accounts: List<Account>, val quote: FxQuote)

/** Lo que la persona eligió en la calculadora, todavía sin validar. [amount] está en la moneda de origen. */
data class ExchangeDraft(
    val direction: ExchangeDirection,
    val amount: String,
    val sourceAccountId: String?,
    val destinationAccountId: String?
)

sealed interface ExchangeValidation {
    data class Valid(val order: ExchangeOrder) : ExchangeValidation

    data class Invalid(val errors: Map<ExchangeField, ExchangeFieldError>) : ExchangeValidation
}

/** El resultado de Continuar: la cotización recién pedida y el cambio validado con ella. */
data class PreparedExchange(val quote: FxQuote, val validation: ExchangeValidation)
