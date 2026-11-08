package com.hacybeyker.cartera.core.domain.market

import java.math.BigDecimal
import java.time.OffsetDateTime

enum class HistoryRange(val wireName: String) {
    DAY("1D"),
    WEEK("1W"),
    MONTH("1M"),
    YEAR("1Y")
}

data class PricePoint(val ts: OffsetDateTime, val price: BigDecimal)

/** Puntos del más antiguo al más reciente; el último empalma con el precio en vivo. */
data class PriceHistory(val symbol: String, val range: HistoryRange, val currency: String, val points: List<PricePoint>)
