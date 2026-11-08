package com.hacybeyker.cartera.feature.exchange.domain.model

import com.hacybeyker.cartera.core.domain.Money
import java.math.BigDecimal

/** El cambio hecho, con los saldos de las dos cuentas después del cambio. */
data class ExchangeReceipt(
    val exchangeId: String,
    val operationNumber: String,
    val quoteId: String,
    val rate: BigDecimal,
    val debited: Money,
    val credited: Money,
    val sourceAccountId: String,
    val sourceAvailableBalance: Money,
    val destinationAccountId: String,
    val destinationAvailableBalance: Money,
    val movementIds: List<String>
)
