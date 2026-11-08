package com.hacybeyker.cartera.feature.transfer.domain.model

/** Lo que la persona escribió en el formulario, todavía sin validar. */
data class TransferDraft(
    val sourceAccountId: String?,
    val destinationAccountNumber: String,
    val amount: String,
    val description: String
)
