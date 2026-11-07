package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.network.dto.ProblemDto
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class ApiCaller @Inject constructor(private val json: Json) {

    suspend operator fun <T> invoke(call: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(call())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (http: HttpException) {
        ApiResult.Failure(http.toApiError())
    } catch (_: InterruptedIOException) {
        ApiResult.Failure(ApiError.Timeout)
    } catch (_: IOException) {
        ApiResult.Failure(ApiError.Network)
    } catch (_: SerializationException) {
        ApiResult.Failure(ApiError.InvalidResponse)
    }

    private fun HttpException.toApiError(): ApiError {
        val body = response()?.errorBody()?.string().orEmpty()
        val problem = try {
            json.decodeFromString<ProblemDto>(body)
        } catch (_: IllegalArgumentException) {
            null
        }
        return problem?.let {
            ApiError.Problem(
                status = code(),
                code = ProblemCode.fromWire(it.code),
                detail = it.detail,
                traceId = it.traceId,
                fieldErrors = it.errors
            )
        } ?: ApiError.Http(code())
    }
}
