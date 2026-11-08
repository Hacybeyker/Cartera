package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.core.data.market.RemoteMarketRepository
import com.hacybeyker.cartera.core.data.market.WebSocketMarketStream
import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.ApiResult
import com.hacybeyker.cartera.core.data.network.BearerInterceptor
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.data.network.MarketApi
import com.hacybeyker.cartera.core.data.network.dto.LoginRequestDto
import com.hacybeyker.cartera.core.data.network.dto.PortfolioDto
import com.hacybeyker.cartera.core.data.network.dto.toPortfolio
import com.hacybeyker.cartera.core.data.portfolio.InMemoryPortfolioChanges
import com.hacybeyker.cartera.core.data.portfolio.RemotePortfolioRepository
import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.demo.backend.CarteraBackend
import com.hacybeyker.cartera.demo.backend.MutableClock
import com.hacybeyker.cartera.demo.backend.fileFixtures
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El cliente de mercado y el canal en vivo, de verdad, contra el backend embebido. */
class MarketClientEndToEndTest {
    private val backend = CarteraBackend(fileFixtures, MutableClock())
    private val scenario = ScenarioStore(FakeScenarioStorage())
    private val tokens = InMemoryAccessTokenStore()
    private val caller = ApiCaller(CarteraHttpClient.json) {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private lateinit var client: OkHttpClient
    private lateinit var api: CarteraApi
    private lateinit var marketApi: MarketApi

    @Before
    fun setUp() {
        backend.start(InetAddress.getLoopbackAddress(), port = 0)
        scenario.update(mapOf("latency" to "0", "tick_ms" to "50"))
        client = CarteraHttpClient.okHttpClient(
            timeoutSeconds = 2,
            interceptors = listOf(BearerInterceptor(tokens), DemoScenarioInterceptor(scenario))
        )
        val retrofit = CarteraHttpClient.retrofit(backend.baseUrl.toString(), client)
        api = retrofit.create(CarteraApi::class.java)
        marketApi = retrofit.create(MarketApi::class.java)
    }

    @After
    fun tearDown() {
        scope.cancel()
        client.dispatcher.executorService.shutdown()
        backend.close()
    }

    @Test
    fun `stocks, history and portfolio match the reference data`() = runBlocking {
        scenario.update(mapOf("tick_ms" to "0"))
        login()
        val market = RemoteMarketRepository(marketApi, caller)
        val portfolio = RemotePortfolioRepository(marketApi, caller, InMemoryPortfolioChanges(), tokens)

        val stocks = success(market.stocks())
        val history = success(market.history("QORI", HistoryRange.DAY))
        val holdings = success(portfolio.portfolio())

        assertEquals("42.10", stocks.first { it.symbol == "ANDM" }.last.toPlainString())
        assertEquals("41.76", success(market.stock("ANDM")).previousClose.toPlainString())
        assertEquals(390, history.points.size)
        assertEquals("115.20", history.points.last().price.toPlainString())
        assertEquals("1094.40", holdings.totals.marketValue.amount.toPlainString())
        assertEquals(holdings, holdings.withQuotes(stocks))
    }

    @Test
    fun `the stream sends the current price and then the ticks`() = runBlocking {
        login()
        val stream = stream()

        val quotes = withTimeout(TIMEOUT_MS) { stream.quotes(setOf("ANDM")).take(3).toList() }

        assertEquals("42.10", quotes.first().last.toPlainString())
        assertTrue(quotes.all { it.symbol == "ANDM" })
        assertEquals(MarketStreamStatus.CONNECTED, stream.status.value)
    }

    @Test
    fun `after some ticks the client values the portfolio like the server`() = runBlocking {
        login()
        val stream = stream()
        val ticks = AtomicInteger()
        val live = scope.launch { stream.quotes(setOf("ANDM", "MOCH", "QORI")).collect { ticks.incrementAndGet() } }
        withTimeout(TIMEOUT_MS) { while (ticks.get() < MIN_TICKS) delay(POLL_MS) }

        val body = client.newCall(Request(requireNotNull(backend.baseUrl.resolve("portfolio")))).execute().use {
            it.body.string()
        }
        val json = CarteraHttpClient.json.parseToJsonElement(body).jsonObject
        val valued = CarteraHttpClient.json.decodeFromJsonElement(PortfolioDto.serializer(), json).toPortfolio().totals
        val server = json.getValue("totals").jsonObject

        assertEquals(server.money("marketValue"), valued.marketValue.amount.toPlainString())
        assertEquals(server.money("gain"), valued.gain.amount.toPlainString())
        assertEquals(server.text("gainPct"), valued.gainPct.toPlainString())
        assertEquals(server.money("dayChange"), valued.dayChange.amount.toPlainString())
        assertEquals(server.text("dayChangePct"), valued.dayChangePct.toPlainString())

        live.cancel()
        withTimeout(TIMEOUT_MS) { while (stream.status.value != MarketStreamStatus.IDLE) delay(POLL_MS) }
    }

    @Test
    fun `a handshake with an invalid token ends the session`() = runBlocking {
        tokens.update("no-es-un-jwt")
        val sessionEnded = AtomicInteger()
        val stream = stream(onSessionExpired = { sessionEnded.incrementAndGet() })
        val live = scope.launch { stream.quotes(setOf("ANDM")).collect {} }

        withTimeout(TIMEOUT_MS) { while (sessionEnded.get() == 0) delay(POLL_MS) }
        live.cancel()

        assertEquals(1, sessionEnded.get())
        assertEquals(MarketStreamStatus.IDLE, stream.status.first())
    }

    private fun stream(onSessionExpired: () -> Unit = {}) = WebSocketMarketStream(
        sockets = client,
        url = backend.baseUrl.newBuilder().addPathSegments("market/stream").build(),
        json = CarteraHttpClient.json,
        tokens = tokens,
        sessionExpiry = onSessionExpired,
        scope = scope,
        reconnectWait = { delay(it) }
    )

    private suspend fun login() {
        val result = caller { api.login(LoginRequestDto("ana.torres", "Cartera2026")) }
        tokens.update((result as ApiResult.Success).value.accessToken)
    }

    private fun <T> success(outcome: Outcome<T>): T = (outcome as Outcome.Success).value

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 20L
        const val MIN_TICKS = 12

        fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

        fun JsonObject.money(key: String) = getValue(key).jsonObject.text("value")
    }
}
