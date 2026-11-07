package com.hacybeyker.cartera.core.ui.preview

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
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
