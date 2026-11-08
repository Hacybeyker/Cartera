package com.hacybeyker.cartera.core.domain.movement

import com.hacybeyker.cartera.core.domain.Money
import java.time.OffsetDateTime

data class Movement(
    val id: String,
    val accountId: String,
    val date: OffsetDateTime,
    val description: String,
    val category: MovementCategory,
    val amount: Money
)

enum class MovementCategory {
    GROCERIES,
    SHOPPING,
    RESTAURANTS,
    TRANSPORT,
    HEALTH,
    ENTERTAINMENT,
    SERVICES,
    SALARY,
    TRANSFER,
    INVESTMENT,
    OTHER;

    companion object {
        fun fromWire(value: String): MovementCategory = entries.firstOrNull { it.name == value } ?: OTHER
    }
}
