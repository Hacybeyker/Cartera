package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.network.dto.FxQuoteDto
import com.hacybeyker.cartera.core.data.network.dto.toFxQuote
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.FxQuote
import java.time.DateTimeException

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404

fun ApiError.toAppError(): AppError = when (this) {
    ApiError.Timeout, ApiError.Network -> AppError.Network
    ApiError.InvalidResponse -> AppError.Server
    is ApiError.Http -> when (status) {
        HTTP_UNAUTHORIZED -> AppError.SessionExpired
        HTTP_NOT_FOUND -> AppError.NotFound
        else -> AppError.Server
    }
    is ApiError.Problem -> code.toAppError(this)
}

// Un 401 sin cuerpo RFC 9457 (un proxy o un servidor que no sigue el contrato) también deja la sesión inservible.
// INVALID_CREDENTIALS e INVALID_DEVICE llegan con cuerpo, así que el login no cuenta como sesión vencida.
val ApiError.isSessionExpiry: Boolean
    get() = when (this) {
        is ApiError.Problem -> code == ProblemCode.SESSION_EXPIRED || code == ProblemCode.UNAUTHORIZED
        is ApiError.Http -> status == HTTP_UNAUTHORIZED
        else -> false
    }

private fun ProblemCode.toAppError(problem: ApiError.Problem): AppError = when (this) {
    ProblemCode.INVALID_CREDENTIALS -> AppError.InvalidCredentials
    ProblemCode.INVALID_DEVICE -> AppError.InvalidDevice
    ProblemCode.SESSION_EXPIRED, ProblemCode.UNAUTHORIZED -> AppError.SessionExpired
    ProblemCode.NOT_FOUND -> AppError.NotFound
    ProblemCode.VALIDATION_ERROR -> AppError.Validation(problem.fieldErrors.associate { it.field to it.code })
    ProblemCode.INSUFFICIENT_FUNDS -> AppError.InsufficientFunds
    ProblemCode.DESTINATION_NOT_FOUND -> AppError.DestinationNotFound
    ProblemCode.CURRENCY_MISMATCH -> AppError.CurrencyMismatch
    ProblemCode.INSUFFICIENT_HOLDINGS -> AppError.InsufficientHoldings
    ProblemCode.QUOTE_EXPIRED -> AppError.QuoteExpired(problem.quote?.toFxQuoteOrNull())
    else -> AppError.Server
}

// Una cotización nueva mal formada no oculta que la anterior venció: la app pide otra con GET /fx/rates.
private fun FxQuoteDto.toFxQuoteOrNull(): FxQuote? = try {
    toFxQuote()
} catch (_: NumberFormatException) {
    null
} catch (_: DateTimeException) {
    null
}

inline fun <T, R> ApiResult<T>.toOutcome(transform: (T) -> R): Outcome<R> = when (this) {
    is ApiResult.Success -> Outcome.Success(transform(value))
    is ApiResult.Failure -> Outcome.Failure(error.toAppError())
}

fun <T> ApiResult<T>.toOutcome(): Outcome<T> = toOutcome { it }
