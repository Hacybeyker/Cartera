package com.hacybeyker.cartera.core.ui.exchange

import androidx.annotation.StringRes
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import java.math.BigDecimal

/** El mensaje de un error de campo del cambio, en la calculadora y bajo el resumen de la confirmación. */
@StringRes
fun ExchangeFieldError.messageRes(field: ExchangeField): Int = when (this) {
    ExchangeFieldError.REQUIRED -> when (field) {
        ExchangeField.SOURCE -> R.string.exchange_error_source_required
        ExchangeField.DESTINATION -> R.string.exchange_error_destination_required
        else -> R.string.exchange_error_required
    }
    ExchangeFieldError.INVALID_FORMAT -> if (field == ExchangeField.AMOUNT) {
        R.string.exchange_error_amount_format
    } else {
        R.string.error_validation
    }
    ExchangeFieldError.MUST_BE_POSITIVE -> R.string.exchange_error_amount_positive
    ExchangeFieldError.EXCEEDS_BALANCE -> R.string.exchange_error_amount_exceeds
    ExchangeFieldError.TOO_SMALL -> R.string.exchange_error_amount_too_small
    ExchangeFieldError.NOT_OWNED -> R.string.exchange_error_account_not_owned
    ExchangeFieldError.NOT_FOUND -> R.string.exchange_error_quote_not_found
    ExchangeFieldError.WRONG_CURRENCY -> R.string.exchange_error_account_currency
}

/** "Compra" si el banco compra los dólares; "Venta" si los vende. */
@StringRes
fun ExchangeDirection.rateLabelRes(): Int = when (this) {
    ExchangeDirection.USD_TO_PEN -> R.string.exchange_rate_buy
    ExchangeDirection.PEN_TO_USD -> R.string.exchange_rate_sell
}

/** Tipo de cambio tal como lo publica el servidor: `3.742`. */
fun BigDecimal.formattedRate(): String = toPlainString()

/** testTags de los campos del cambio y de sus errores (`<campo>_error`), en la calculadora y en la confirmación. */
object ExchangeFieldTestTags {
    const val AMOUNT = "cambio_monto"
    const val SOURCE = "cambio_origen"
    const val DESTINATION = "cambio_destino"
    const val QUOTE = "cambio_cotizacion"

    fun field(field: ExchangeField): String = when (field) {
        ExchangeField.AMOUNT -> AMOUNT
        ExchangeField.SOURCE -> SOURCE
        ExchangeField.DESTINATION -> DESTINATION
        ExchangeField.QUOTE -> QUOTE
    }

    fun fieldError(field: ExchangeField) = "${field(field)}_error"
}
