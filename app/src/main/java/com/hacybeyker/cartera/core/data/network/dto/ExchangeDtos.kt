package com.hacybeyker.cartera.core.data.network.dto

import kotlinx.serialization.Serializable

/** `amount` va en la moneda de la cuenta de origen. */
@Serializable
data class ExchangeRequestDto(
    val quoteId: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: MoneyDto
)

@Serializable
data class ExchangeResponseDto(
    val exchangeId: String,
    val operationNumber: String,
    val createdAt: String,
    val quoteId: String,
    val rate: String,
    val debited: MoneyDto,
    val credited: MoneyDto,
    val sourceAccountId: String,
    val sourceAvailableBalance: MoneyDto,
    val destinationAccountId: String,
    val destinationAvailableBalance: MoneyDto,
    val movementIds: List<String>
)
