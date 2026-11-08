package com.hacybeyker.cartera.core.domain.market

import java.math.BigDecimal
import java.time.OffsetDateTime

/** Cotización de un par de divisas: [buy] es lo que paga el banco por cada unidad; [sell], lo que cobra. */
data class FxQuote(
    val pair: String,
    val buy: BigDecimal,
    val sell: BigDecimal,
    val quoteId: String,
    val expiresAt: OffsetDateTime,
    val ts: OffsetDateTime
)
