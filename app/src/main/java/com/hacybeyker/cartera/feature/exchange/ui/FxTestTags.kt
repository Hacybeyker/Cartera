package com.hacybeyker.cartera.feature.exchange.ui

import com.hacybeyker.cartera.core.domain.AppError

object FxTestTags {
    const val CONFIRMATION = "cambio_confirmacion"
    const val CONFIRM = "cambio_confirmar"
    const val CANCEL = "cambio_cancelar"
    const val DEBIT = "cambio_confirmacion_debito"
    const val CREDIT = "cambio_confirmacion_credito"
    const val RATE = "cambio_confirmacion_tasa"
    const val EXPIRY = "cambio_vencimiento"
    const val MARK_CONFIRMATION = "cambio_confirmacion"

    /** Modal con la cotización nueva: `_aceptar` la usa y `_salir` vuelve a Cambio. */
    const val QUOTE_EXPIRED = "modal_cotizacion_vencida"
    const val QUOTE_EXPIRED_ACCEPT = "modal_cotizacion_vencida_aceptar"
    const val QUOTE_EXPIRED_EXIT = "modal_cotizacion_vencida_salir"

    const val RESULT = "cambio_resultado"
    const val RESULT_OPERATION = "cambio_resultado_operacion"
    const val RESULT_DEBITED = "cambio_resultado_debitado"
    const val RESULT_CREDITED = "cambio_resultado_acreditado"
    const val RESULT_RATE = "cambio_resultado_tasa"
    const val RESULT_SOURCE_BALANCE = "cambio_resultado_saldo_origen"
    const val RESULT_DESTINATION_BALANCE = "cambio_resultado_saldo_destino"
    const val RESULT_ERROR = "cambio_resultado_error"
    const val BACK = "cambio_volver"
    const val MARK_RESULT = "cambio_resultado"

    /** Un modal por cada rechazo de negocio, con su botón `_aceptar`; los mismos nombres que en la transferencia. */
    fun rejection(error: AppError): String = when (error) {
        AppError.InsufficientFunds -> "modal_fondos_insuficientes"
        AppError.CurrencyMismatch -> "modal_moneda_distinta"
        else -> "modal_cambio_rechazado"
    }
}
