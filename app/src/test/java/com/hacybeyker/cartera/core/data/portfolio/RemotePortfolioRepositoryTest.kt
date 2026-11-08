package com.hacybeyker.cartera.core.data.portfolio

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.marketApi
import com.hacybeyker.cartera.core.testing.problemJson
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemotePortfolioRepositoryTest {
    private val server = MockWebServer()
    private val changes = InMemoryPortfolioChanges()
    private var token: String? = "token-1"
    private lateinit var repository: RemotePortfolioRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemotePortfolioRepository(
            api = server.marketApi(),
            call = ApiCaller(CarteraHttpClient.json) {},
            changes = changes,
            tokens = { token }
        )
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `maps the holdings and derives the change per share`() = runTest {
        server.enqueueJson(200, portfolio(QORI))

        val portfolio = (repository.portfolio() as Outcome.Success).value
        val qori = portfolio.holdings.single()

        assertEquals("ACC-003", portfolio.accountId)
        assertEquals(2, qori.quantity)
        assertEquals("120.00", qori.averagePrice.toPlainString())
        assertEquals("115.20", qori.lastPrice.toPlainString())
        assertEquals("-0.36", qori.priceChange.toPlainString())
        assertEquals(Money("-9.60".toBigDecimal(), "USD"), portfolio.totals.gain)
        assertEquals("/api/v1/portfolio", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `the portfolio is cached until an order changes the holdings`() = runTest {
        server.enqueueJson(200, portfolio(QORI))
        server.enqueueJson(200, portfolio())

        repository.portfolio()
        repository.portfolio()
        assertEquals(1, server.requestCount)

        changes.notifyChanged()
        val refreshed = (repository.portfolio() as Outcome.Success).value

        assertEquals(2, server.requestCount)
        assertEquals(1, changes.revision)
        assertEquals(emptyList<Any>(), refreshed.holdings)
    }

    @Test
    fun `another session never sees the previous portfolio`() = runTest {
        server.enqueueJson(200, portfolio(QORI))
        server.enqueueJson(200, portfolio())

        repository.portfolio()
        token = "token-2"
        repository.portfolio()

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `failures are not cached`() = runTest {
        server.enqueueJson(500, problemJson("INTERNAL_ERROR", 500), "application/problem+json")
        server.enqueueJson(200, portfolio(QORI))

        assertEquals(Outcome.Failure(AppError.Server), repository.portfolio())
        assertEquals(1, (repository.portfolio() as Outcome.Success).value.holdings.size)
    }

    @Test
    fun `a holding without shares is a server error, not a crash`() = runTest {
        server.enqueueJson(200, portfolio(QORI.replace("\"quantity\":2", "\"quantity\":0")))

        assertEquals(Outcome.Failure(AppError.Server), repository.portfolio())
    }

    private fun portfolio(vararg holdings: String) =
        """{"accountId":"ACC-003","currency":"USD","holdings":[${holdings.joinToString(",")}],"totals":{}}"""

    private companion object {
        const val QORI = """{"symbol":"QORI","name":"Qori Gold","quantity":2,""" +
            """"averagePrice":{"value":"120.00","currency":"USD"},"lastPrice":{"value":"115.20","currency":"USD"},""" +
            """"marketValue":{"value":"230.40","currency":"USD"},"dayChange":{"value":"-0.72","currency":"USD"}}"""
    }
}
