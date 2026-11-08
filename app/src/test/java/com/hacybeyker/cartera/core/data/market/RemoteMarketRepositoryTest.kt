package com.hacybeyker.cartera.core.data.market

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.marketApi
import com.hacybeyker.cartera.core.testing.problemJson
import java.time.OffsetDateTime
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteMarketRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteMarketRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteMarketRepository(server.marketApi(), ApiCaller(CarteraHttpClient.json) {})
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `lists the stocks with their prices`() = runTest {
        server.enqueueJson(200, """{"items":[$ANDM]}""")

        val quote = (repository.stocks() as Outcome.Success).value.single()

        assertEquals("ANDM", quote.symbol)
        assertEquals("42.10", quote.last.toPlainString())
        assertEquals("0.34", quote.change.toPlainString())
        assertEquals("0.81", quote.changePct.toPlainString())
        assertEquals(OffsetDateTime.parse("2026-11-07T10:00:00-05:00"), quote.ts)
        assertEquals("/api/v1/market/stocks", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `reads one stock with the data of the day`() = runTest {
        server.enqueueJson(200, ANDM_DETAIL)

        val detail = (repository.stock("ANDM") as Outcome.Success).value

        assertEquals("42.10", detail.quote.last.toPlainString())
        assertEquals("41.76", detail.previousClose.toPlainString())
        assertEquals("42.22", detail.high.toPlainString())
        assertEquals(3_643_000L, detail.volume)
        assertEquals("/api/v1/market/stocks/ANDM", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `reads the history of a range`() = runTest {
        server.enqueueJson(200, HISTORY)

        val history = (repository.history("QORI", HistoryRange.DAY) as Outcome.Success).value

        assertEquals(HistoryRange.DAY, history.range)
        assertEquals(listOf("115.10", "115.20"), history.points.map { it.price.toPlainString() })
        val request = server.takeRequest().url
        assertEquals("/api/v1/market/stocks/QORI/history", request.encodedPath)
        assertEquals("1D", request.queryParameter("range"))
    }

    @Test
    fun `a missing stock is not found`() = runTest {
        server.enqueueJson(404, problemJson("NOT_FOUND", 404), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.NotFound), repository.stock("NOPE"))
    }

    @Test
    fun `malformed prices, dates or ranges are a server error, not a crash`() = runTest {
        server.enqueueJson(200, """{"items":[${ANDM.replace("\"42.10\"", "\"cuarenta\"")}]}""")
        server.enqueueJson(200, ANDM_DETAIL.replace("2026-11-07T10:00:00-05:00", "hoy"))
        server.enqueueJson(200, HISTORY.replace("\"1D\"", "\"5Y\""))

        assertEquals(Outcome.Failure(AppError.Server), repository.stocks())
        assertEquals(Outcome.Failure(AppError.Server), repository.stock("ANDM"))
        assertEquals(Outcome.Failure(AppError.Server), repository.history("QORI", HistoryRange.DAY))
    }

    private companion object {
        const val ANDM = """{"symbol":"ANDM","name":"Andina Minerals","currency":"USD","last":"42.10",""" +
            """"change":"0.34","changePct":"0.81","ts":"2026-11-07T10:00:00-05:00"}"""
        const val ANDM_DETAIL = """{"symbol":"ANDM","name":"Andina Minerals","currency":"USD","last":"42.10",""" +
            """"change":"0.34","changePct":"0.81","ts":"2026-11-07T10:00:00-05:00","open":"41.54","high":"42.22",""" +
            """"low":"41.12","previousClose":"41.76","volume":3643000,"description":"Minera."}"""
        const val HISTORY = """{"symbol":"QORI","range":"1D","currency":"USD","points":[""" +
            """{"ts":"2026-11-07T09:59:00-05:00","price":"115.10"},{"ts":"2026-11-07T10:00:00-05:00","price":"115.20"}]}"""
    }
}
