package com.hacybeyker.cartera.core.domain.exchange

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.market.FxQuote
import java.math.BigDecimal
import java.time.OffsetDateTime

/** Lo que el cambio muestra de una cuenta propia. */
data class ExchangeAccount(val id: String, val alias: String, val numberMasked: String, val currency: String) {
    val label: String get() = "$alias $numberMasked".trim()

    companion object {
        fun from(account: Account) = ExchangeAccount(
            id = account.id,
            alias = account.alias,
            numberMasked = account.numberMasked,
            currency = account.currency
        )
    }
}

/**
 * Un cambio validado y listo para confirmar, con la cotización que lo respalda ([quoteId], la [rate] que aplica según
 * el sentido y su vencimiento). [idempotencyKey] identifica este envío y se reutiliza en cada Reintentar.
 */
data class ExchangeOrder(
    val direction: ExchangeDirection,
    val quoteId: String,
    val rate: BigDecimal,
    val expiresAt: OffsetDateTime,
    val source: ExchangeAccount,
    val destination: ExchangeAccount,
    val amount: Money,
    val idempotencyKey: String
) {
    /** Lo que se acreditará en el destino, calculado como el servidor. */
    val credit: Money get() = Money(direction.credit(amount.amount, rate), destination.currency)

    /**
     * El mismo cambio con otra cotización. Es otro cuerpo, así que viaja con otra clave: un servidor que compara el
     * cuerpo de una clave repetida lo rechazaría.
     */
    fun withQuote(quote: FxQuote, idempotencyKey: String) = copy(
        quoteId = quote.quoteId,
        rate = direction.rate(quote),
        expiresAt = quote.expiresAt,
        idempotencyKey = idempotencyKey
    )
}
