package com.hacybeyker.cartera.core.domain.market

import java.math.BigDecimal
import java.time.OffsetDateTime

/** Precio vigente de una acción, en [currency]. [change] y [changePct] son contra el cierre anterior. */
data class Quote(
    val symbol: String,
    val name: String,
    val currency: String,
    val last: BigDecimal,
    val change: BigDecimal,
    val changePct: BigDecimal,
    val ts: OffsetDateTime
)
