package com.hacybeyker.cartera.feature.movements.domain.model

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import java.math.BigDecimal

data class Breakdown(
    val lines: List<BreakdownLine>,
    val fees: Money,
    val taxes: Money,
    val exchangeRate: BigDecimal?,
    val merchant: Merchant?,
    val category: MovementCategory
)

data class BreakdownLine(val concept: String, val amount: Money)

data class Merchant(val name: String, val city: String)
