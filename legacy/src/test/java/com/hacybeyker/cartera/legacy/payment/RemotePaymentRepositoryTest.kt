package com.hacybeyker.cartera.legacy.payment

import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.core.data.LegacyJson
import com.hacybeyker.cartera.legacy.core.di.LegacyNetworkModule
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.LegacyException
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import com.hacybeyker.cartera.legacy.payment.data.RemotePaymentRepository
import com.hacybeyker.cartera.legacy.payment.data.UuidIdempotencyKeyFactory
import com.hacybeyker.cartera.legacy.payment.domain.PaymentOrder
import com.hacybeyker.cartera.legacy.testing.RecordingSessionExpiry
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.pen
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemotePaymentRepositoryTest {
    private val server = MockWebServer()
    private val expiry = RecordingSessionExpiry()
    private lateinit var repository: RemotePaymentRepository

    private val order = PaymentOrder(
        billerId = "BIL-LUZ",
        customerCode = "100200",
        debtIds = listOf("DEB-LUZ-202610", "DEB-LUZ-202611"),
        accountId = "ACC-002",
        pin = "1234",
        idempotencyKey = "key-1"
    )

    @Before
    fun setUp() {
        server.start()
        val retrofit = LegacyNetworkModule.provideRetrofit(
            LegacyHttpClient { OkHttpClient() },
            object : LegacyApiConfig {
                override val baseUrl: String = server.url("/api/v1/").toString()
            }
        )
        repository = RemotePaymentRepository(
            LegacyNetworkModule.providePaymentsApi(retrofit),
            LegacyApiCaller(LegacyJson.instance, expiry)
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `the payment posts the five fields with the idempotency key and maps the 201 of the contract`() {
        server.enqueue(json(201, CREATED))

        val payment = repository.pay(order).blockingGet()

        assertEquals("PAY-000001", payment.paymentId)
        assertEquals("80000001", payment.operationNumber)
        assertEquals(pen("177.50"), payment.total)
        assertEquals(pen("2122.50"), payment.newBalance)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/bill-payments", request.target)
        assertEquals("key-1", request.headers["Idempotency-Key"])
        assertEquals(
            """{"billerId":"BIL-LUZ","customerCode":"100200","debtIds":["DEB-LUZ-202610","DEB-LUZ-202611"],""" +
                """"accountId":"ACC-002","pin":"1234"}""",
            request.body?.utf8()
        )
    }

    @Test
    fun `a retry sends the same body with the same key`() {
        server.enqueue(MockResponse.Builder().code(503).build())
        server.enqueue(json(201, CREATED))

        repository.pay(order).test().assertError { (it as LegacyException).error == LegacyError.Server }
        repository.pay(order).blockingGet()

        val first = server.takeRequest()
        val second = server.takeRequest()
        assertEquals(first.headers["Idempotency-Key"], second.headers["Idempotency-Key"])
        assertEquals(first.body?.utf8(), second.body?.utf8())
    }

    @Test
    fun `every rejection of the contract keeps its code`() {
        listOf("INVALID_PIN", "INSUFFICIENT_FUNDS", "DEBT_ALREADY_PAID", "VALIDATION_ERROR", "CURRENCY_MISMATCH")
            .forEach { code ->
                server.enqueue(json(422, problem(code, 422), "application/problem+json"))

                repository.pay(order).test().assertError {
                    val error = (it as LegacyException).error as LegacyError.Rejected
                    error.code == ProblemCode.valueOf(code) && error.status == 422
                }
            }
    }

    @Test
    fun `a 401 reports the expired session`() {
        server.enqueue(json(401, problem("UNAUTHORIZED", 401), "application/problem+json"))

        repository.pay(order).test().assertError { (it as LegacyException).error == LegacyError.SessionExpired }

        assertEquals(1, expiry.calls)
    }

    @Test
    fun `the voucher is read as html with the bearer client of the app`() {
        server.enqueue(
            MockResponse.Builder().code(200).addHeader("Content-Type", "text/html; charset=utf-8")
                .body(Samples.voucherHtml).build()
        )

        val html = repository.voucher("PAY-000001").blockingGet()

        assertEquals(Samples.voucherHtml, html)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/bill-payments/PAY-000001/voucher", request.target)
        assertEquals("text/html", request.headers["Accept"])
    }

    @Test
    fun `an unknown payment is a rejection and an expired session is reported`() {
        server.enqueue(json(404, problem("PAYMENT_NOT_FOUND", 404), "application/problem+json"))
        server.enqueue(json(401, problem("UNAUTHORIZED", 401), "application/problem+json"))

        repository.voucher("PAY-9").test().assertError {
            ((it as LegacyException).error as LegacyError.Rejected).code == ProblemCode.PAYMENT_NOT_FOUND
        }
        repository.voucher("PAY-9").test().assertError { (it as LegacyException).error == LegacyError.SessionExpired }

        assertEquals(1, expiry.calls)
    }

    @Test
    fun `the idempotency keys are random uuids and never repeat`() {
        val factory = UuidIdempotencyKeyFactory()

        val keys = List(50) { factory.create() }

        assertEquals(50, keys.toSet().size)
        assertTrue(keys.all { Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(it) })
        assertNotEquals(keys[0], keys[1])
    }

    private fun json(code: Int, body: String, type: String = "application/json") =
        MockResponse.Builder().code(code).addHeader("Content-Type", type).body(body).build()

    private fun problem(code: String, status: Int) =
        """{"type":"https://cartera.invalid/problems/x","title":"t","status":$status,"detail":"detalle",""" +
            """"code":"$code","traceId":"req-1"}"""

    private companion object {
        const val CREATED =
            """{"paymentId":"PAY-000001","operationNumber":"80000001","createdAt":"2026-11-08T15:04:05Z",""" +
                """"paidDebts":[{"id":"DEB-LUZ-202610","period":"2026-10",""" +
                """"amount":{"value":"85.40","currency":"PEN"}},""" +
                """{"id":"DEB-LUZ-202611","period":"2026-11","amount":{"value":"92.10","currency":"PEN"}}],""" +
                """"total":{"value":"177.50","currency":"PEN"},"accountId":"ACC-002",""" +
                """"accountAvailableBalance":{"value":"2122.50","currency":"PEN"},"movementId":"MOV-002-P0001"}"""
    }
}
