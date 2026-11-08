package com.hacybeyker.cartera.feature.order.data

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.marketApi
import com.hacybeyker.cartera.core.testing.problemJson
import com.hacybeyker.cartera.feature.order.ReferenceBuy
import com.hacybeyker.cartera.feature.order.ReferenceBuyReceipt
import com.hacybeyker.cartera.feature.order.data.repository.RemoteOrderRepository
import com.hacybeyker.cartera.feature.order.domain.model.OrderHolding
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteOrderRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteOrderRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteOrderRepository(server.marketApi(), ApiCaller(CarteraHttpClient.json) {})
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `sends the order with its idempotency key and maps the receipt`() = runTest {
        server.enqueueJson(201, receipt())

        val outcome = repository.place(ReferenceBuy)

        assertEquals(Outcome.Success(ReferenceBuyReceipt), outcome)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/orders", request.url.encodedPath)
        assertEquals("key-1", request.headers["Idempotency-Key"])
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("ANDM", body["symbol"]!!.jsonPrimitive.content)
        assertEquals("BUY", body["side"]!!.jsonPrimitive.content)
        assertEquals("2", body["quantity"]!!.jsonPrimitive.content)
        assertEquals(false, body["quantity"]!!.jsonPrimitive.isString)
        assertEquals("ACC-003", body["accountId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a sell that empties the holding has no average price`() = runTest {
        server.enqueueJson(
            201,
            receipt(side = "SELL", holding = """{"symbol":"ANDM","quantity":0,"averagePrice":null}""")
        )

        val receipt = (repository.place(ReferenceBuy.copy(side = OrderSide.SELL)) as Outcome.Success).value

        assertEquals(OrderSide.SELL, receipt.side)
        assertEquals(OrderHolding(quantity = 0, averagePrice = null), receipt.holding)
    }

    @Test
    fun `each business rejection maps to its own error`() = runTest {
        server.enqueueJson(422, problemJson("INSUFFICIENT_FUNDS", 422), "application/problem+json")
        server.enqueueJson(422, problemJson("INSUFFICIENT_HOLDINGS", 422), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.InsufficientFunds), repository.place(ReferenceBuy))
        assertEquals(Outcome.Failure(AppError.InsufficientHoldings), repository.place(ReferenceBuy))
    }

    @Test
    fun `a validation error keeps the code of each field`() = runTest {
        server.enqueueJson(
            422,
            """{"type":"about:blank","title":"t","status":422,"detail":"d","code":"VALIDATION_ERROR",
            "traceId":"req-1","errors":[{"field":"accountId","code":"UNSUPPORTED_CURRENCY","message":"m"}]}""",
            "application/problem+json"
        )

        assertEquals(
            Outcome.Failure(AppError.Validation(mapOf("accountId" to "UNSUPPORTED_CURRENCY"))),
            repository.place(ReferenceBuy)
        )
    }

    @Test
    fun `a side or an amount outside the contract is a server error, not a crash`() = runTest {
        server.enqueueJson(201, receipt(side = "HOLD"))
        server.enqueueJson(201, receipt(total = "mucho"))

        assertEquals(Outcome.Failure(AppError.Server), repository.place(ReferenceBuy))
        assertEquals(Outcome.Failure(AppError.Server), repository.place(ReferenceBuy))
    }

    private fun receipt(
        side: String = "BUY",
        total: String = "84.20",
        holding: String = """{"symbol":"ANDM","quantity":12,"averagePrice":{"value":"40.35","currency":"USD"}}"""
    ) = """{"orderId":"ORD-000001","operationNumber":"70000001","createdAt":"2026-11-08T10:00:00-05:00",
        "side":"$side","symbol":"ANDM","quantity":2,"executedPrice":{"value":"42.10","currency":"USD"},
        "total":{"value":"$total","currency":"USD"},"accountId":"ACC-003",
        "accountAvailableBalance":{"value":"966.00","currency":"USD"},"movementId":"MOV-003-O0001",
        "holding":$holding}"""
}
