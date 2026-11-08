package com.hacybeyker.cartera.feature.transfer.domain.model

import com.hacybeyker.cartera.core.domain.Money

/**
 * Una transferencia validada y lista para confirmar. [idempotencyKey] identifica esta operación: se reutiliza en
 * cada reintento para que el servidor no cobre dos veces.
 */
data class TransferOrder(
    val sourceAccountId: String,
    val sourceAlias: String,
    val sourceNumberMasked: String,
    val destinationAccountNumber: String,
    val amount: Money,
    val description: String?,
    val idempotencyKey: String
)
