package com.hacybeyker.cartera.legacy.core.domain

/** Por qué falló una llamada, ya traducido: ni los repositorios ni los Presenters ven excepciones de red. */
sealed interface LegacyError {
    /** Sin conexión, tiempo agotado o corte a mitad de la respuesta. */
    data object Network : LegacyError

    /** El servidor falló (5xx) o respondió algo que el módulo no entiende. */
    data object Server : LegacyError

    /** El servidor rechazó el token (401), con o sin cuerpo RFC 9457. */
    data object SessionExpired : LegacyError

    /** Un rechazo del negocio (4xx con cuerpo RFC 9457). */
    data class Rejected(val status: Int, val code: ProblemCode, val detail: String, val fieldErrors: List<FieldError>) :
        LegacyError
}

data class FieldError(val field: String, val code: String, val message: String)

enum class ProblemCode {
    VALIDATION_ERROR,
    BILLER_NOT_FOUND,
    CUSTOMER_NOT_FOUND,
    PAYMENT_NOT_FOUND,
    INVALID_PIN,
    DEBT_ALREADY_PAID,
    INSUFFICIENT_FUNDS,
    CURRENCY_MISMATCH,
    NOT_FOUND,
    UNKNOWN;

    companion object {
        fun fromWire(code: String): ProblemCode = entries.firstOrNull { it.name == code } ?: UNKNOWN
    }
}

/** Lleva un [LegacyError] por el `onError` de Rx. */
class LegacyException(val error: LegacyError) : RuntimeException(error.toString())
