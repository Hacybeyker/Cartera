package com.hacybeyker.cartera.legacy.core.data

import com.hacybeyker.cartera.legacy.core.data.dto.ProblemDto
import com.hacybeyker.cartera.legacy.core.domain.FieldError
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.LegacyException
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.integration.LegacySessionExpiry
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleTransformer
import java.io.IOException
import java.time.DateTimeException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_SERVER_ERROR = 500

/**
 * Traduce los fallos de una llamada a un [LegacyException]. Todas las llamadas pasan por aquí, así que es el único
 * lugar que avisa de una sesión vencida.
 */
class LegacyApiCaller(private val json: Json, private val sessionExpiry: LegacySessionExpiry) {

    fun <T : Any> translated(): SingleTransformer<T, T> = SingleTransformer { upstream ->
        upstream.onErrorResumeNext { failure -> Single.error(translate(failure)) }
    }

    private fun translate(failure: Throwable): Throwable {
        val error = failure.toLegacyError() ?: return failure
        if (error == LegacyError.SessionExpired) sessionExpiry.onSessionExpired()
        return LegacyException(error)
    }

    private fun Throwable.toLegacyError(): LegacyError? = when (this) {
        is LegacyException -> error
        is HttpException -> toLegacyError()
        is IOException -> LegacyError.Network
        // Un monto o una fecha mal formados al mapear el DTO. Cualquier otra excepción es un error de programación y
        // no se disfraza de error del servicio.
        is SerializationException, is NumberFormatException, is DateTimeException -> LegacyError.Server
        else -> null
    }

    private fun HttpException.toLegacyError(): LegacyError {
        val status = code()
        return when {
            status == HTTP_UNAUTHORIZED -> LegacyError.SessionExpired
            status >= HTTP_SERVER_ERROR -> LegacyError.Server
            else -> response()?.errorBody()?.string().orEmpty().toProblem(status) ?: LegacyError.Server
        }
    }

    private fun String.toProblem(status: Int): LegacyError.Rejected? {
        val problem = try {
            json.decodeFromString(ProblemDto.serializer(), this)
        } catch (_: SerializationException) {
            return null
        }
        return LegacyError.Rejected(
            status = status,
            code = ProblemCode.fromWire(problem.code),
            detail = problem.detail,
            fieldErrors = problem.errors.map { FieldError(it.field, it.code, it.message) }
        )
    }
}
