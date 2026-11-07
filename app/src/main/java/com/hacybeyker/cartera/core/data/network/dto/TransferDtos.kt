package com.hacybeyker.cartera.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransferRequestDto(
    val sourceAccountId: String,
    val destinationAccountNumber: String,
    val amount: MoneyDto,
    val description: String? = null
)

@Serializable
data class TransferResponseDto(
    val transferId: String,
    val operationNumber: String,
    val createdAt: String,
    val sourceAccountId: String,
    val sourceAvailableBalance: MoneyDto,
    val movementId: String
)
