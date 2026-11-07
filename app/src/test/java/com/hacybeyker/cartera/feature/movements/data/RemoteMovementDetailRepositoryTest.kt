package com.hacybeyker.cartera.feature.movements.data

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.problemJson
import com.hacybeyker.cartera.feature.movements.data.repository.RemoteMovementDetailRepository
import com.hacybeyker.cartera.feature.movements.domain.model.Merchant
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RemoteMovementDetailRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteMovementDetailRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteMovementDetailRepository(server.carteraApi(), ApiCaller(CarteraHttpClient.json) {})
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `maps the movement detail`() = runTest {
        server.enqueueJson(
            200,
            """{"id":"MOV-002-0001","accountId":"ACC-002","date":"2026-11-06T14:32:00-05:00",
            "description":"Compra","category":"GROCERIES","amount":{"value":"-1379.23","currency":"PEN"},
            "type":"DEBIT","operationNumber":"20000001","channel":"POS","status":"COMPLETED",
            "counterparty":{"name":"Supermercado La Canasta","reference":"VISA ****9034"}}"""
        )

        val detail = (repository.detail("MOV-002-0001") as Outcome.Success).value

        assertEquals("/api/v1/movements/MOV-002-0001", server.takeRequest().url.encodedPath)
        assertEquals("MOV-002-0001", detail.movement.id)
        assertEquals("20000001", detail.operationNumber)
        assertEquals("Supermercado La Canasta", detail.counterpartyName)
        assertEquals("VISA ****9034", detail.counterpartyReference)
    }

    @Test
    fun `maps the breakdown with and without merchant or exchange rate`() = runTest {
        server.enqueueJson(200, breakdown(merchant = """{"name":"La Canasta","city":"Lima"}""", rate = null))
        server.enqueueJson(200, breakdown(merchant = "null", rate = "\"3.7520\""))

        val withMerchant = (repository.breakdown("MOV-002-0001") as Outcome.Success).value
        val withRate = (repository.breakdown("MOV-003-0001") as Outcome.Success).value

        assertEquals("/api/v1/movements/MOV-002-0001/breakdown", server.takeRequest().url.encodedPath)
        assertEquals(Merchant("La Canasta", "Lima"), withMerchant.merchant)
        assertNull(withMerchant.exchangeRate)
        assertEquals(1, withMerchant.lines.size)
        assertEquals(MovementCategory.GROCERIES, withMerchant.category)
        assertNull(withRate.merchant)
        assertEquals("3.7520".toBigDecimal(), withRate.exchangeRate)
    }

    @Test
    fun `an unknown movement is not found`() = runTest {
        server.enqueueJson(404, problemJson("NOT_FOUND", 404), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.NotFound), repository.detail("MOV-999"))
    }

    private fun breakdown(merchant: String, rate: String?) =
        """{"lines":[{"concept":"Arroz","amount":{"value":"9.53","currency":"PEN"}}],
        "fees":{"value":"0.00","currency":"PEN"},"taxes":{"value":"1.00","currency":"PEN"},
        ${rate?.let { "\"exchangeRate\":$it," }.orEmpty()}"merchant":$merchant,"category":"GROCERIES"}"""
}
