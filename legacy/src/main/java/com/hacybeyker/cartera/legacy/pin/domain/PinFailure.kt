package com.hacybeyker.cartera.legacy.pin.domain

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode

/** Un rechazo del pago que se avisa con un diálogo de un solo botón y no se arregla con otra clave. */
enum class PinNotice { INSUFFICIENT_FUNDS, DEBT_ALREADY_PAID, REJECTED }

/** Qué hace la pantalla de la clave con un fallo del pago. */
sealed interface PinFailure {
    /** El servidor rechazó el token: se cierra el módulo. */
    data object SessionExpired : PinFailure

    /** La clave no es la de la persona: mensaje, puntos vacíos y teclado reordenado. */
    data object WrongPin : PinFailure

    /** Un rechazo de negocio o de validación: un aviso con Aceptar. */
    data class Rejected(val notice: PinNotice) : PinFailure

    /** Red, tiempo agotado o error del servidor: Reintentar con la misma orden o Cancelar. */
    data class Retryable(val error: LegacyError) : PinFailure
}

fun LegacyError.toPinFailure(): PinFailure = when (this) {
    LegacyError.SessionExpired -> PinFailure.SessionExpired
    is LegacyError.Rejected -> when (code) {
        ProblemCode.INVALID_PIN -> PinFailure.WrongPin
        ProblemCode.INSUFFICIENT_FUNDS -> PinFailure.Rejected(PinNotice.INSUFFICIENT_FUNDS)
        ProblemCode.DEBT_ALREADY_PAID -> PinFailure.Rejected(PinNotice.DEBT_ALREADY_PAID)
        else -> PinFailure.Rejected(PinNotice.REJECTED)
    }
    else -> PinFailure.Retryable(this)
}
