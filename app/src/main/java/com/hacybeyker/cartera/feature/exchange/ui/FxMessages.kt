package com.hacybeyker.cartera.feature.exchange.ui

import androidx.annotation.StringRes
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.ui.messageRes

/** Rechazos de negocio que tienen su propio modal; el resto se muestra en el modal de error con Reintentar. */
internal val AppError.isExchangeRejection: Boolean
    get() = this == AppError.InsufficientFunds || this == AppError.CurrencyMismatch

@StringRes
internal fun AppError.rejectionTitleRes(): Int = when (this) {
    AppError.InsufficientFunds -> R.string.exchange_rejected_funds_title
    AppError.CurrencyMismatch -> R.string.exchange_rejected_currency_title
    else -> R.string.exchange_error_title
}

@StringRes
internal fun AppError.rejectionMessageRes(): Int = when (this) {
    AppError.InsufficientFunds -> R.string.exchange_rejected_funds_message
    AppError.CurrencyMismatch -> R.string.exchange_rejected_currency_message
    else -> messageRes()
}
