package com.hacybeyker.cartera.core.ui.preview

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import com.hacybeyker.cartera.core.domain.portfolio.Holding
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
import java.time.OffsetDateTime

// Datos de los @Preview, tomados de los fixtures del backend de demo.

private fun pen(value: String) = Money(value.toBigDecimal(), "PEN")

val previewAccounts = listOf(
    Account("ACC-001", "Cuenta Sueldo", "SAVINGS", "PEN", "****4821", pen("8450.75"), pen("8450.75")),
    Account("ACC-002", "Cuenta Gastos", "CHECKING", "PEN", "****9034", pen("2300.00"), pen("2300.00")),
    Account(
        "ACC-003",
        "Cuenta Dólares",
        "SAVINGS",
        "USD",
        "****1177",
        Money("1050.20".toBigDecimal(), "USD"),
        Money("1050.20".toBigDecimal(), "USD")
    )
)

val previewMovements = listOf(
    Movement(
        id = "MOV-002-0001",
        accountId = "ACC-002",
        date = OffsetDateTime.parse("2026-11-06T14:32:00-05:00"),
        description = "Compra Supermercado La Canasta",
        category = MovementCategory.GROCERIES,
        amount = pen("-1379.23")
    ),
    Movement(
        id = "MOV-002-0002",
        accountId = "ACC-002",
        date = OffsetDateTime.parse("2026-11-05T09:10:00-05:00"),
        description = "Clínica San Gabriel",
        category = MovementCategory.HEALTH,
        amount = pen("-180.00")
    ),
    Movement(
        id = "MOV-001-0001",
        accountId = "ACC-001",
        date = OffsetDateTime.parse("2026-11-01T08:00:00-05:00"),
        description = "Abono de sueldo",
        category = MovementCategory.SALARY,
        amount = pen("5200.00")
    )
)

private val previewQuoteTime = OffsetDateTime.parse("2026-11-07T10:00:00-05:00")

private fun quote(symbol: String, name: String, last: String, change: String, changePct: String) = Quote(
    symbol = symbol,
    name = name,
    currency = "USD",
    last = last.toBigDecimal(),
    change = change.toBigDecimal(),
    changePct = changePct.toBigDecimal(),
    ts = previewQuoteTime
)

/** Las ocho acciones a su precio inicial, con la variación contra el cierre anterior de los fixtures. */
val previewQuotes = listOf(
    quote("ANDM", "Andina Minerals", "42.10", "0.34", "0.81"),
    quote("CUSC", "Cusco Telecom", "27.30", "-0.46", "-1.66"),
    quote("INKA", "Inka Energy", "63.40", "-1.95", "-2.98"),
    quote("LIMA", "Lima Logistics", "9.85", "-0.28", "-2.76"),
    quote("MOCH", "Moche Tech", "88.60", "-2.37", "-2.61"),
    quote("NAZC", "Nazca Retail", "12.45", "-0.04", "-0.32"),
    quote("PCFC", "Pacífico Foods", "18.75", "-0.14", "-0.74"),
    quote("QORI", "Qori Gold", "115.20", "-0.36", "-0.31")
)

private fun holding(symbol: String, name: String, quantity: Int, averagePrice: String): Holding {
    val quote = previewQuotes.first { it.symbol == symbol }
    return Holding(symbol, name, quantity, "USD", averagePrice.toBigDecimal(), quote.last, quote.change)
}

/** El portafolio de Ana a los precios iniciales. */
val previewPortfolio = Portfolio(
    accountId = "ACC-003",
    currency = "USD",
    holdings = listOf(
        holding("ANDM", "Andina Minerals", 10, "40.00"),
        holding("MOCH", "Moche Tech", 5, "80.00"),
        holding("QORI", "Qori Gold", 2, "120.00")
    )
)

/** Una jornada de QORI resumida en 30 puntos, del más antiguo al más reciente; termina en su precio inicial. */
val previewHistoryPrices = listOf(
    "115.56", "115.70", "115.92", "115.81", "116.04", "116.33", "116.18", "115.95", "115.62", "115.40",
    "115.47", "115.21", "114.98", "114.76", "114.90", "115.12", "115.35", "115.28", "115.04", "114.85",
    "114.62", "114.71", "114.93", "115.10", "115.02", "115.26", "115.44", "115.31", "115.12", "115.20"
).map { it.toBigDecimal() }
