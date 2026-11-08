package com.hacybeyker.cartera.feature.transfer.ui

import androidx.annotation.StringRes
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferFieldError

@StringRes
internal fun TransferFieldError.messageRes(field: TransferField): Int = when (this) {
    TransferFieldError.REQUIRED -> if (field == TransferField.SOURCE) {
        R.string.transfer_error_source_required
    } else {
        R.string.transfer_error_required
    }
    TransferFieldError.INVALID_FORMAT -> when (field) {
        TransferField.DESTINATION -> R.string.transfer_error_destination_format
        TransferField.AMOUNT -> R.string.transfer_error_amount_format
        else -> R.string.error_validation
    }
    TransferFieldError.MUST_BE_POSITIVE -> R.string.transfer_error_amount_positive
    TransferFieldError.EXCEEDS_BALANCE -> R.string.transfer_error_amount_exceeds
    TransferFieldError.TOO_LONG -> R.string.transfer_error_description_long
    TransferFieldError.NOT_OWNED -> R.string.transfer_error_source_not_owned
}

/** Rechazos de negocio que tienen su propio modal; el resto se muestra en el modal de error con Reintentar. */
internal val AppError.isTransferRejection: Boolean
    get() = this == AppError.InsufficientFunds ||
        this == AppError.DestinationNotFound ||
        this == AppError.CurrencyMismatch

@StringRes
internal fun AppError.rejectionTitleRes(): Int = when (this) {
    AppError.InsufficientFunds -> R.string.transfer_rejected_funds_title
    AppError.DestinationNotFound -> R.string.transfer_rejected_destination_title
    AppError.CurrencyMismatch -> R.string.transfer_rejected_currency_title
    else -> R.string.transfer_error_title
}
