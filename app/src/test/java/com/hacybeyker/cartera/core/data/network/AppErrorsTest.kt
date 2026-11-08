package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.network.dto.FieldErrorDto
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorsTest {

    @Test
    fun `transport errors map to network and server`() {
        assertEquals(AppError.Network, ApiError.Timeout.toAppError())
        assertEquals(AppError.Network, ApiError.Network.toAppError())
        assertEquals(AppError.Server, ApiError.InvalidResponse.toAppError())
    }

    @Test
    fun `http statuses without a problem body map by status`() {
        assertEquals(AppError.SessionExpired, ApiError.Http(401).toAppError())
        assertEquals(AppError.NotFound, ApiError.Http(404).toAppError())
        assertEquals(AppError.Server, ApiError.Http(502).toAppError())
    }

    @Test
    fun `problem codes map to what the app does with them`() {
        assertEquals(AppError.InvalidCredentials, problem(ProblemCode.INVALID_CREDENTIALS).toAppError())
        assertEquals(AppError.SessionExpired, problem(ProblemCode.SESSION_EXPIRED).toAppError())
        assertEquals(AppError.SessionExpired, problem(ProblemCode.UNAUTHORIZED).toAppError())
        assertEquals(AppError.NotFound, problem(ProblemCode.NOT_FOUND).toAppError())
        assertEquals(AppError.Server, problem(ProblemCode.SERVICE_UNAVAILABLE).toAppError())
        assertEquals(AppError.InsufficientFunds, problem(ProblemCode.INSUFFICIENT_FUNDS).toAppError())
        assertEquals(AppError.DestinationNotFound, problem(ProblemCode.DESTINATION_NOT_FOUND).toAppError())
        assertEquals(AppError.CurrencyMismatch, problem(ProblemCode.CURRENCY_MISMATCH).toAppError())
        assertEquals(AppError.InsufficientHoldings, problem(ProblemCode.INSUFFICIENT_HOLDINGS).toAppError())
        assertEquals(AppError.Server, problem(ProblemCode.UNKNOWN).toAppError())
    }

    @Test
    fun `validation keeps the code of each field`() {
        val error = problem(ProblemCode.VALIDATION_ERROR, listOf(FieldErrorDto("username", "REQUIRED", "m")))

        assertEquals(AppError.Validation(mapOf("username" to "REQUIRED")), error.toAppError())
    }

    @Test
    fun `session expired, unauthorized and a bare 401 count as session expiry`() {
        assertTrue(problem(ProblemCode.SESSION_EXPIRED).isSessionExpiry)
        assertTrue(problem(ProblemCode.UNAUTHORIZED).isSessionExpiry)
        assertTrue(ApiError.Http(401).isSessionExpiry)
    }

    @Test
    fun `invalid credentials and other errors do not count as session expiry`() {
        assertFalse(problem(ProblemCode.INVALID_CREDENTIALS).isSessionExpiry)
        assertFalse(ApiError.Http(403).isSessionExpiry)
        assertFalse(ApiError.Http(500).isSessionExpiry)
        assertFalse(ApiError.Network.isSessionExpiry)
    }

    @Test
    fun `api result converts to an outcome`() {
        assertEquals(Outcome.Success(2), ApiResult.Success(1).toOutcome { it + 1 })
        assertEquals(Outcome.Failure(AppError.Network), ApiResult.Failure(ApiError.Network).toOutcome { it })
    }

    private fun problem(code: ProblemCode, fields: List<FieldErrorDto> = emptyList()) =
        ApiError.Problem(status = 400, code = code, detail = "d", traceId = "t", fieldErrors = fields)
}
