package com.hacybeyker.cartera.feature.transfer.data

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.problemJson
import com.hacybeyker.cartera.feature.transfer.ReferenceOrder
import com.hacybeyker.cartera.feature.transfer.ReferenceReceipt
import com.hacybeyker.cartera.feature.transfer.data.repository.RemoteTransferRepository
import com.hacybeyker.cartera.feature.transfer.data.repository.UuidIdempotencyKeyFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class RemoteTransferRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteTransferRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteTransferRepository(server.carteraApi(), ApiCaller(CarteraHttpClient.json) {})
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `sends the order with its idempotency key and maps the receipt`() = runTest {
        server.enqueueJson(
            201,
            """{"transferId":"TRF-000001","operationNumber":"90000001","createdAt":"2026-11-07T10:00:00-05:00",
            "sourceAccountId":"ACC-002","sourceAvailableBalance":{"value":"2150.00","currency":"PEN"},
            "movementId":"MOV-002-T0001"}"""
        )

        val outcome = repository.transfer(ReferenceOrder.copy(description = "Almuerzo"))

        assertEquals(Outcome.Success(ReferenceReceipt), outcome)
        val request = server.takeRequest()
        assertEquals("/api/v1/transfers", request.url.encodedPath)
        assertEquals("key-1", request.headers["Idempotency-Key"])
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("ACC-002", body["sourceAccountId"]!!.jsonPrimitive.content)
        assertEquals("999-0000001-0-01", body["destinationAccountNumber"]!!.jsonPrimitive.content)
        assertEquals("150.00", body["amount"]!!.jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals("PEN", body["amount"]!!.jsonObject["currency"]!!.jsonPrimitive.content)
        assertEquals("Almuerzo", body["description"]!!.jsonPrimitive.content)
    }

    @Test
    fun `without description the field is not sent`() = runTest {
        server.enqueueJson(422, problemJson("INSUFFICIENT_FUNDS", 422), "application/problem+json")

        repository.transfer(ReferenceOrder)

        assertFalse("description" in Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject)
    }

    @Test
    fun `each 422 code maps to its own error`() = runTest {
        server.enqueueJson(422, problemJson("INSUFFICIENT_FUNDS", 422), "application/problem+json")
        server.enqueueJson(422, problemJson("DESTINATION_NOT_FOUND", 422), "application/problem+json")
        server.enqueueJson(422, problemJson("CURRENCY_MISMATCH", 422), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.InsufficientFunds), repository.transfer(ReferenceOrder))
        assertEquals(Outcome.Failure(AppError.DestinationNotFound), repository.transfer(ReferenceOrder))
        assertEquals(Outcome.Failure(AppError.CurrencyMismatch), repository.transfer(ReferenceOrder))
    }

    @Test
    fun `uuid keys are new each time`() {
        val keys = UuidIdempotencyKeyFactory()

        assertNotEquals(keys.newKey(), keys.newKey())
    }
}
