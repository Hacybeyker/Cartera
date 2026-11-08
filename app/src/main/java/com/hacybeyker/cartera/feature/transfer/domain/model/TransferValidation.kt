package com.hacybeyker.cartera.feature.transfer.domain.model

sealed interface TransferValidation {
    data class Valid(val order: TransferOrder) : TransferValidation

    data class Invalid(val errors: Map<TransferField, TransferFieldError>) : TransferValidation
}
