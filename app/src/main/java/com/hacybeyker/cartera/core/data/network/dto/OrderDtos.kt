package com.hacybeyker.cartera.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class OrderRequestDto(val symbol: String, val side: String, val quantity: Int, val accountId: String)

@Serializable
data class OrderHoldingDto(val symbol: String, val quantity: Int, val averagePrice: MoneyDto?)

@Serializable
data class OrderResponseDto(
    val orderId: String,
    val operationNumber: String,
    val createdAt: String,
    val side: String,
    val symbol: String,
    val quantity: Int,
    val executedPrice: MoneyDto,
    val total: MoneyDto,
    val accountId: String,
    val accountAvailableBalance: MoneyDto,
    val movementId: String,
    val holding: OrderHoldingDto
)
