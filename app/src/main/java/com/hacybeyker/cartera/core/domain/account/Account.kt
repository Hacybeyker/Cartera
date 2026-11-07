package com.hacybeyker.cartera.core.domain.account

import com.hacybeyker.cartera.core.domain.Money

data class Account(
    val id: String,
    val alias: String,
    val type: String,
    val currency: String,
    val numberMasked: String,
    val availableBalance: Money,
    val accountingBalance: Money
)
