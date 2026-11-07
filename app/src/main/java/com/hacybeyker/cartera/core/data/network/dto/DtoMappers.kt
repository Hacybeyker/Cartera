package com.hacybeyker.cartera.core.data.network.dto

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import java.time.OffsetDateTime

fun MoneyDto.toMoney(): Money = Money(amount = value.toBigDecimal(), currency = currency)

fun AccountDto.toAccount(): Account = Account(
    id = id,
    alias = alias,
    type = type,
    currency = currency,
    numberMasked = numberMasked,
    availableBalance = availableBalance.toMoney(),
    accountingBalance = accountingBalance.toMoney()
)

fun MovementDto.toMovement(): Movement = Movement(
    id = id,
    accountId = accountId,
    date = OffsetDateTime.parse(date),
    description = description,
    category = MovementCategory.fromWire(category),
    amount = amount.toMoney()
)

fun MovementDetailDto.toMovement(): Movement = Movement(
    id = id,
    accountId = accountId,
    date = OffsetDateTime.parse(date),
    description = description,
    category = MovementCategory.fromWire(category),
    amount = amount.toMoney()
)
