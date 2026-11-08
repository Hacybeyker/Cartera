package com.hacybeyker.cartera.feature.order.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.feature.order.domain.model.OrderField

object OrderTestTags {
    const val FORM = "orden_pantalla"
    const val PRICE = "orden_precio"
    const val QUANTITY = "orden_cantidad"
    const val ACCOUNT = "orden_cuenta"
    const val TOTAL = "orden_total"
    const val CONTINUE = "orden_continuar"
    const val RECONNECTING = "orden_reconectando"

    /** Prefijo de los estados del formulario (`orden_cargando`, `_error`, `_reintentar`). */
    const val STATES = "orden"
    const val MARK_FORM = "orden_formulario"

    const val CONFIRMATION = "orden_confirmacion"
    const val CONFIRM = "orden_confirmar"
    const val CANCEL = "orden_cancelar"
    const val MARK_CONFIRMATION = "orden_confirmacion"

    const val RESULT = "orden_resultado"
    const val RESULT_OPERATION = "orden_resultado_operacion"
    const val RESULT_PRICE = "orden_resultado_precio"
    const val RESULT_TOTAL = "orden_resultado_total"
    const val RESULT_BALANCE = "orden_resultado_saldo"
    const val RESULT_HOLDING = "orden_resultado_tenencia"
    const val RESULT_ERROR = "orden_resultado_error"
    const val BACK_TO_MARKET = "orden_volver_mercado"
    const val MARK_RESULT = "orden_resultado"

    /** El error de un campo, en el formulario y en la confirmación: `<campo>_error`. */
    fun fieldError(field: OrderField) = "${field(field)}_error"

    fun field(field: OrderField): String = when (field) {
        OrderField.SYMBOL -> "orden_accion"
        OrderField.SIDE -> "orden_lado"
        OrderField.QUANTITY -> QUANTITY
        OrderField.ACCOUNT -> ACCOUNT
    }

    /** Un modal por cada rechazo de negocio, con su botón `_aceptar`; los mismos nombres que en la transferencia. */
    fun rejection(error: AppError): String = when (error) {
        AppError.InsufficientFunds -> "modal_fondos_insuficientes"
        AppError.InsufficientHoldings -> "modal_tenencia_insuficiente"
        else -> "modal_orden_rechazada"
    }
}
