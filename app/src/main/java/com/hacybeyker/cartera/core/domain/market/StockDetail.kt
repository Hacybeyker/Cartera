package com.hacybeyker.cartera.core.domain.market

import java.math.BigDecimal

/** Una acción con los datos del día. */
data class StockDetail(
    val quote: Quote,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val previousClose: BigDecimal,
    val volume: Long,
    val description: String
)
