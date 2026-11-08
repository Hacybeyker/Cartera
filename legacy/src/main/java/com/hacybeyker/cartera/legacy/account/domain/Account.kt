package com.hacybeyker.cartera.legacy.account.domain

import com.hacybeyker.cartera.legacy.core.domain.Money

data class Account(val id: String, val alias: String, val numberMasked: String, val availableBalance: Money) {
    val currency: String get() = availableBalance.currency
}
