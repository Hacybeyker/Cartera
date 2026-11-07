package com.hacybeyker.cartera.core.domain

sealed interface AppError {
    /** Timeout o desconexión: no llegó respuesta. */
    data object Network : AppError

    /** 5xx, backend caído o una respuesta que no se pudo leer. */
    data object Server : AppError

    data object InvalidCredentials : AppError

    /** Token vencido o ausente en una llamada autenticada. */
    data object SessionExpired : AppError

    data object NotFound : AppError

    data class Validation(val fieldCodes: Map<String, String>) : AppError
}
