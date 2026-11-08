package com.hacybeyker.cartera.feature.order.ui

import androidx.annotation.StringRes
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.domain.model.OrderFieldError

@StringRes
internal fun OrderFieldError.messageRes(field: OrderField): Int = when (this) {
    OrderFieldError.REQUIRED -> if (field == OrderField.ACCOUNT) {
        R.string.order_error_account_required
    } else {
        R.string.order_error_required
    }
    OrderFieldError.INVALID_FORMAT -> if (field == OrderField.QUANTITY) {
        R.string.order_error_quantity_format
    } else {
        R.string.error_validation
    }
    OrderFieldError.MUST_BE_POSITIVE -> R.string.order_error_quantity_positive
    OrderFieldError.EXCEEDS_BALANCE -> R.string.order_error_exceeds_balance
    OrderFieldError.EXCEEDS_HOLDINGS -> R.string.order_error_exceeds_holdings
    OrderFieldError.NOT_OWNED -> R.string.order_error_account_not_owned
    OrderFieldError.UNSUPPORTED_CURRENCY -> R.string.order_error_account_currency
    OrderFieldError.UNKNOWN_SYMBOL -> R.string.order_error_unknown_symbol
}

/** Rechazos de negocio que tienen su propio modal; el resto se muestra en el modal de error con Reintentar. */
internal val AppError.isOrderRejection: Boolean
    get() = this == AppError.InsufficientFunds || this == AppError.InsufficientHoldings

@StringRes
internal fun AppError.rejectionTitleRes(): Int = when (this) {
    AppError.InsufficientFunds -> R.string.order_rejected_funds_title
    AppError.InsufficientHoldings -> R.string.order_rejected_holdings_title
    else -> R.string.order_error_title
}

@StringRes
internal fun AppError.rejectionMessageRes(): Int = when (this) {
    AppError.InsufficientFunds -> R.string.order_rejected_funds_message
    else -> messageRes()
}

@StringRes
internal fun OrderSide.labelRes(): Int = when (this) {
    OrderSide.BUY -> R.string.order_side_buy
    OrderSide.SELL -> R.string.order_side_sell
}
