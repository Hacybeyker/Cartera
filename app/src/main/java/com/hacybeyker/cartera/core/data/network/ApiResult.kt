package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.network.dto.FieldErrorDto
import com.hacybeyker.cartera.core.data.network.dto.FxQuoteDto

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

sealed interface ApiError {
    data object Timeout : ApiError

    data object Network : ApiError

    data object InvalidResponse : ApiError

    data class Http(val status: Int) : ApiError

    data class Problem(
        val status: Int,
        val code: ProblemCode,
        val detail: String,
        val traceId: String,
        val fieldErrors: List<FieldErrorDto>,
        /** Solo en `QUOTE_EXPIRED`: la cotización nueva que ofrece el servidor. */
        val quote: FxQuoteDto? = null
    ) : ApiError
}

enum class ProblemCode {
    INVALID_CREDENTIALS,
    SESSION_EXPIRED,
    UNAUTHORIZED,
    NOT_FOUND,
    VALIDATION_ERROR,
    INSUFFICIENT_FUNDS,
    DESTINATION_NOT_FOUND,
    CURRENCY_MISMATCH,
    INSUFFICIENT_HOLDINGS,
    QUOTE_EXPIRED,
    INTERNAL_ERROR,
    SERVICE_UNAVAILABLE,
    UNKNOWN;

    companion object {
        fun fromWire(code: String): ProblemCode = entries.firstOrNull { it.name == code } ?: UNKNOWN
    }
}
