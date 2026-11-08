package com.hacybeyker.cartera.core.domain

import com.hacybeyker.cartera.core.domain.market.FxQuote

sealed interface AppError {
    /** Timeout o desconexión: no llegó respuesta. */
    data object Network : AppError

    /** 5xx, backend caído o una respuesta que no se pudo leer. */
    data object Server : AppError

    data object InvalidCredentials : AppError

    /** El `deviceToken` del ingreso con huella no existe o fue revocado. */
    data object InvalidDevice : AppError

    /** Token vencido o ausente en una llamada autenticada. */
    data object SessionExpired : AppError

    data object NotFound : AppError

    data class Validation(val fieldCodes: Map<String, String>) : AppError

    /** El monto supera el saldo disponible del origen. */
    data object InsufficientFunds : AppError

    /** No hay una cuenta de terceros con ese número. */
    data object DestinationNotFound : AppError

    /** El origen y el destino usan monedas distintas. */
    data object CurrencyMismatch : AppError

    /** Se quiere vender más acciones de las que se tienen. */
    data object InsufficientHoldings : AppError

    /** La cotización del cambio venció. [quote] es la nueva que ofrece el servidor, si llegó bien formada. */
    data class QuoteExpired(val quote: FxQuote?) : AppError
}
