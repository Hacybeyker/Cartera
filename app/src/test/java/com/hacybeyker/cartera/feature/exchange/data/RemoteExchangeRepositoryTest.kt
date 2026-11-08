package com.hacybeyker.cartera.feature.exchange.data

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.marketApi
import com.hacybeyker.cartera.core.testing.problemJson
import com.hacybeyker.cartera.feature.exchange.ReferenceExchange
import com.hacybeyker.cartera.feature.exchange.ReferenceExchangeReceipt
import com.hacybeyker.cartera.feature.exchange.data.repository.RemoteExchangeRepository
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteExchangeRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteExchangeRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteExchangeRepository(server.marketApi(), ApiCaller(CarteraHttpClient.json) {})
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `sends the exchange with its quote and idempotency key and maps the receipt`() = runTest {
        server.enqueueJson(201, receipt())

        val outcome = repository.exchange(ReferenceExchange)

        assertEquals(Outcome.Success(ReferenceExchangeReceipt), outcome)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/fx/exchanges", request.url.encodedPath)
        assertEquals("key-0", request.headers["Idempotency-Key"])
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("QTE-000001", body["quoteId"]!!.jsonPrimitive.content)
        assertEquals("ACC-003", body["sourceAccountId"]!!.jsonPrimitive.content)
        assertEquals("ACC-002", body["destinationAccountId"]!!.jsonPrimitive.content)
        val amount = body["amount"]!!.jsonObject
        assertEquals("100.00", amount["value"]!!.jsonPrimitive.content)
        assertEquals("USD", amount["currency"]!!.jsonPrimitive.content)
    }

    @Test
    fun `each rejection maps to its own error and an expired quote brings the new one`() = runTest {
        server.enqueueJson(422, problemJson("INSUFFICIENT_FUNDS", 422), "application/problem+json")
        server.enqueueJson(422, problemJson("CURRENCY_MISMATCH", 422), "application/problem+json")
        server.enqueueJson(
            422,
            problemJson("QUOTE_EXPIRED", 422).dropLast(1) + ""","quote":$QUOTE}""",
            "application/problem+json"
        )

        assertEquals(Outcome.Failure(AppError.InsufficientFunds), repository.exchange(ReferenceExchange))
        assertEquals(Outcome.Failure(AppError.CurrencyMismatch), repository.exchange(ReferenceExchange))
        val expired = (repository.exchange(ReferenceExchange) as Outcome.Failure).error as AppError.QuoteExpired
        assertEquals("QTE-000002", expired.quote?.quoteId)
        assertEquals(BigDecimal("3.745"), expired.quote?.buy)
    }

    @Test
    fun `a validation error keeps the code of each field`() = runTest {
        server.enqueueJson(
            422,
            """{"type":"about:blank","title":"t","status":422,"detail":"d","code":"VALIDATION_ERROR",
            "traceId":"req-1","errors":[{"field":"amount.value","code":"TOO_SMALL","message":"m"}]}""",
            "application/problem+json"
        )

        assertEquals(
            Outcome.Failure(AppError.Validation(mapOf("amount.value" to "TOO_SMALL"))),
            repository.exchange(ReferenceExchange)
        )
    }

    @Test
    fun `an amount outside the contract is a server error, not a crash`() = runTest {
        server.enqueueJson(201, receipt(credited = "mucho"))

        assertEquals(Outcome.Failure(AppError.Server), repository.exchange(ReferenceExchange))
    }

    private fun receipt(credited: String = "374.20") =
        """{"exchangeId":"FX-000001","operationNumber":"80000001","createdAt":"2026-11-07T10:00:10-05:00",
        "quoteId":"QTE-000001","rate":"3.742","debited":{"value":"100.00","currency":"USD"},
        "credited":{"value":"$credited","currency":"PEN"},"sourceAccountId":"ACC-003",
        "sourceAvailableBalance":{"value":"950.20","currency":"USD"},"destinationAccountId":"ACC-002",
        "destinationAvailableBalance":{"value":"2674.20","currency":"PEN"},
        "movementIds":["MOV-003-X0001","MOV-002-X0001"]}"""

    private companion object {
        const val QUOTE = """{"pair":"USD/PEN","buy":"3.745","sell":"3.771","quoteId":"QTE-000002",""" +
            """"expiresAt":"2026-11-07T10:00:40-05:00","ts":"2026-11-07T10:00:10-05:00"}"""
    }
}
