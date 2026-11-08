package com.hacybeyker.cartera.legacy.account.data.dto

import com.hacybeyker.cartera.legacy.account.domain.Account
import com.hacybeyker.cartera.legacy.core.data.dto.MoneyDto
import com.hacybeyker.cartera.legacy.core.data.dto.toMoney
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(val id: String, val alias: String, val numberMasked: String, val availableBalance: MoneyDto)

@Serializable
data class AccountListDto(val items: List<AccountDto>)

fun AccountDto.toAccount(): Account = Account(id, alias, numberMasked, availableBalance.toMoney())
