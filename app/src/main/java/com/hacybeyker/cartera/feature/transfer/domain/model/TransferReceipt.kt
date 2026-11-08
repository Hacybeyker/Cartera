package com.hacybeyker.cartera.feature.transfer.domain.model

import com.hacybeyker.cartera.core.domain.Money

data class TransferReceipt(
    val transferId: String,
    val operationNumber: String,
    val sourceAccountId: String,
    val sourceAvailableBalance: Money,
    val movementId: String
)
