package com.hacybeyker.cartera.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MovementDto(
    val id: String,
    val accountId: String,
    val date: String,
    val description: String,
    val category: String,
    val amount: MoneyDto,
    val type: String
)

@Serializable
data class MovementPageDto(val items: List<MovementDto>, val nextCursor: String?)

@Serializable
data class MovementDetailDto(
    val id: String,
    val accountId: String,
    val date: String,
    val description: String,
    val category: String,
    val amount: MoneyDto,
    val type: String,
    val operationNumber: String,
    val channel: String,
    val status: String,
    val counterparty: CounterpartyDto
)

@Serializable
data class CounterpartyDto(val name: String, val reference: String)

@Serializable
data class BreakdownDto(
    val lines: List<BreakdownLineDto>,
    val fees: MoneyDto,
    val taxes: MoneyDto,
    val exchangeRate: String? = null,
    val merchant: MerchantDto?,
    val category: String
)

@Serializable
data class BreakdownLineDto(val concept: String, val amount: MoneyDto)

@Serializable
data class MerchantDto(val name: String, val city: String)
