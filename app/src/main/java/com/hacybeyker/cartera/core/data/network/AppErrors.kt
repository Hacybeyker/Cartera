package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome

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

val ApiError.isSessionExpiry: Boolean
    get() = this is ApiError.Problem && (code == ProblemCode.SESSION_EXPIRED || code == ProblemCode.UNAUTHORIZED)

private fun ProblemCode.toAppError(problem: ApiError.Problem): AppError = when (this) {
    ProblemCode.INVALID_CREDENTIALS -> AppError.InvalidCredentials
    ProblemCode.SESSION_EXPIRED, ProblemCode.UNAUTHORIZED -> AppError.SessionExpired
    ProblemCode.NOT_FOUND -> AppError.NotFound
    ProblemCode.VALIDATION_ERROR -> AppError.Validation(problem.fieldErrors.associate { it.field to it.code })
    else -> AppError.Server
}

inline fun <T, R> ApiResult<T>.toOutcome(transform: (T) -> R): Outcome<R> = when (this) {
    is ApiResult.Success -> Outcome.Success(transform(value))
    is ApiResult.Failure -> Outcome.Failure(error.toAppError())
}
