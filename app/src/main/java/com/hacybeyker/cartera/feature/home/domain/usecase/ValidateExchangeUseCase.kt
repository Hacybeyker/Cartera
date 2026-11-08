package com.hacybeyker.cartera.feature.home.domain.usecase

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.exchange.ExchangeAccount
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.core.domain.exchange.parseExchangeAmount
import com.hacybeyker.cartera.core.domain.idempotency.IdempotencyKeyFactory
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeDraft
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeValidation
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Valida en el cliente lo mismo que el contrato: monto mayor que cero y con 2 decimales como máximo, que alcance el
 * saldo del origen y que acredite al menos un centavo, y origen y destino propios y de las monedas del sentido elegido.
 * Si todo está bien, el cambio recibe una `Idempotency-Key` nueva: cada Continuar es una operación distinta.
 */
class ValidateExchangeUseCase @Inject constructor(private val keys: IdempotencyKeyFactory) {

    fun errors(draft: ExchangeDraft, accounts: List<Account>, quote: FxQuote): Map<ExchangeField, ExchangeFieldError> {
        val source = accounts.firstOrNull { it.id == draft.sourceAccountId }
        val destination = accounts.firstOrNull { it.id == draft.destinationAccountId }
        return buildMap {
            accountError(draft.sourceAccountId, source, draft.direction.sourceCurrency)?.let {
                put(ExchangeField.SOURCE, it)
            }
            accountError(draft.destinationAccountId, destination, draft.direction.destinationCurrency)?.let {
                put(ExchangeField.DESTINATION, it)
            }
            amountError(draft, source, quote)?.let { put(ExchangeField.AMOUNT, it) }
        }
    }

    operator fun invoke(draft: ExchangeDraft, accounts: List<Account>, quote: FxQuote): ExchangeValidation {
        val errors = errors(draft, accounts, quote)
        if (errors.isNotEmpty()) return ExchangeValidation.Invalid(errors)
        // Sin errores, las dos cuentas existen y el monto tiene formato.
        val source = accounts.first { it.id == draft.sourceAccountId }
        val destination = accounts.first { it.id == draft.destinationAccountId }
        val amount = requireNotNull(parse(draft.amount))
        return ExchangeValidation.Valid(
            ExchangeOrder(
                direction = draft.direction,
                quoteId = quote.quoteId,
                rate = draft.direction.rate(quote),
                expiresAt = quote.expiresAt,
                source = ExchangeAccount.from(source),
                destination = ExchangeAccount.from(destination),
                amount = Money(amount.setScale(2), source.currency),
                idempotencyKey = keys.newKey()
            )
        )
    }

    private fun accountError(id: String?, account: Account?, currency: String): ExchangeFieldError? = when {
        id == null -> ExchangeFieldError.REQUIRED
        account == null -> ExchangeFieldError.NOT_OWNED
        account.currency != currency -> ExchangeFieldError.WRONG_CURRENCY
        else -> null
    }

    private fun amountError(draft: ExchangeDraft, source: Account?, quote: FxQuote): ExchangeFieldError? {
        val text = draft.amount.trim()
        val amount = parse(text)
        return when {
            text.isEmpty() -> ExchangeFieldError.REQUIRED
            amount == null -> ExchangeFieldError.INVALID_FORMAT
            amount.signum() <= 0 -> ExchangeFieldError.MUST_BE_POSITIVE
            source != null && amount > source.availableBalance.amount -> ExchangeFieldError.EXCEEDS_BALANCE
            draft.direction.credit(amount, draft.direction.rate(quote)).signum() <= 0 -> ExchangeFieldError.TOO_SMALL
            else -> null
        }
    }

    private fun parse(text: String): BigDecimal? = parseExchangeAmount(text)
}
