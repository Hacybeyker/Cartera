package com.hacybeyker.cartera.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val alias: String,
    val type: String,
    val currency: String,
    val numberMasked: String,
    val availableBalance: MoneyDto,
    val accountingBalance: MoneyDto
)

@Serializable
data class AccountListDto(val items: List<AccountDto>)
