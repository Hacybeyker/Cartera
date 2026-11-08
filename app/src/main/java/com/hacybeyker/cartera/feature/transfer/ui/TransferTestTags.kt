package com.hacybeyker.cartera.feature.transfer.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField

object TransferTestTags {
    const val FORM = "transferir_pantalla"
    const val SOURCE = "transferir_origen"
    const val DESTINATION = "transferir_destino"
    const val AMOUNT = "transferir_monto"
    const val DESCRIPTION = "transferir_descripcion"
    const val CONTINUE = "transferir_continuar"

    /** Prefijo de los estados del formulario (`transferir_cargando`, `_error`, `_reintentar`). */
    const val STATES = "transferir"
    const val MARK_FORM = "transferir_formulario"

    const val CONFIRMATION = "transferir_confirmacion"
    const val CONFIRM = "transferir_confirmar"
    const val CANCEL = "transferir_cancelar"
    const val MARK_CONFIRMATION = "transferir_confirmacion"

    const val RESULT = "transferir_resultado"
    const val RESULT_OPERATION = "transferir_resultado_operacion"
    const val RESULT_BALANCE = "transferir_resultado_saldo"
    const val RESULT_ERROR = "transferir_resultado_error"
    const val BACK_TO_HOME = "transferir_volver_inicio"
    const val MARK_RESULT = "transferir_resultado"

    fun source(accountId: String) = "transferir_origen_item_$accountId"

    /** El error de un campo, en el formulario y en la confirmación: `<campo>_error`. */
    fun fieldError(field: TransferField) = "${field(field)}_error"

    fun field(field: TransferField): String = when (field) {
        TransferField.SOURCE -> SOURCE
        TransferField.DESTINATION -> DESTINATION
        TransferField.AMOUNT -> AMOUNT
        TransferField.DESCRIPTION -> DESCRIPTION
    }

    /** Un modal por cada rechazo de negocio, con su botón `_aceptar`. */
    fun rejection(error: AppError): String = when (error) {
        AppError.InsufficientFunds -> "modal_fondos_insuficientes"
        AppError.DestinationNotFound -> "modal_destino_inexistente"
        AppError.CurrencyMismatch -> "modal_moneda_distinta"
        else -> "modal_transferencia_rechazada"
    }
}
