package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.core.data.account.InMemoryAccountChanges
import com.hacybeyker.cartera.core.data.account.RemoteAccountRepository
import com.hacybeyker.cartera.core.data.market.RemoteMarketRepository
import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.ApiResult
import com.hacybeyker.cartera.core.data.network.BearerInterceptor
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.data.network.MarketApi
import com.hacybeyker.cartera.core.data.network.dto.LoginRequestDto
import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.GetAccountsRevisionUseCase
import com.hacybeyker.cartera.core.domain.account.GetAccountsUseCase
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.core.domain.market.ObserveFxQuotesUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.testing.FakeMarketStream
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.SequentialKeys
import com.hacybeyker.cartera.demo.backend.CarteraBackend
import com.hacybeyker.cartera.demo.backend.MutableClock
import com.hacybeyker.cartera.demo.backend.fileFixtures
import com.hacybeyker.cartera.feature.exchange.data.repository.RemoteExchangeRepository
import com.hacybeyker.cartera.feature.exchange.domain.usecase.ConfirmExchangeUseCase
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeDraft
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeValidation
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadExchangeUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.StartExchangeUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.ValidateExchangeUseCase
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeAccountRole
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeAction
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeViewModel
import java.math.BigDecimal
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** El cambio de referencia (`tick_ms 0`, datos limpios) y la cotización vencida, con el cliente de verdad. */
class ExchangeClientEndToEndTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock()
    private val backend = CarteraBackend(fileFixtures, clock)
    private val scenario = ScenarioStore(FakeScenarioStorage())
    private val tokens = InMemoryAccessTokenStore()
    private val caller = ApiCaller(CarteraHttpClient.json) {}
    private val calls = ConcurrentHashMap<String, AtomicInteger>()
    private val accountChanges = InMemoryAccountChanges()
    private val keys = SequentialKeys()
    private lateinit var client: OkHttpClient
    private lateinit var api: CarteraApi
    private lateinit var marketApi: MarketApi

    private val accounts by lazy { RemoteAccountRepository(api, caller, accountChanges, tokens) }
    private val market by lazy { RemoteMarketRepository(marketApi, caller) }
    private val validate = ValidateExchangeUseCase(keys)
    private val start by lazy { StartExchangeUseCase(market, validate) }
    private val exchanges by lazy { RemoteExchangeRepository(marketApi, caller) }
    private val confirm by lazy { ConfirmExchangeUseCase(exchanges, accountChanges) }

    /** US$ 100 de la Cuenta Dólares (ACC-003) a la Cuenta Gastos (ACC-002). */
    private val reference = ExchangeDraft(ExchangeDirection.USD_TO_PEN, "100", "ACC-003", "ACC-002")

    @Before
    fun setUp() {
        backend.start(InetAddress.getLoopbackAddress(), port = 0)
        scenario.update(mapOf("latency" to "0", "tick_ms" to "0"))
        val counter = Interceptor { chain ->
            val request = chain.request()
            calls.getOrPut("${request.method} ${request.url.encodedPath.removePrefix("/api/v1/")}") {
                AtomicInteger()
            }.incrementAndGet()
            chain.proceed(request)
        }
        client = CarteraHttpClient.okHttpClient(
            timeoutSeconds = 2,
            interceptors = listOf(BearerInterceptor(tokens), DemoScenarioInterceptor(scenario), counter)
        )
        val retrofit = CarteraHttpClient.retrofit(backend.baseUrl.toString(), client)
        api = retrofit.create(CarteraApi::class.java)
        marketApi = retrofit.create(MarketApi::class.java)
    }

    @After
    fun tearDown() {
        client.dispatcher.executorService.shutdown()
        backend.close()
    }

    @Test
    fun `the reference exchange credits S$ 374,20 and leaves both balances as the reference says`() = runTest {
        login()
        val overview = success(LoadExchangeUseCase(accounts, market)())
        assertEquals("3.742", overview.quote.buy.toPlainString())
        assertEquals("3.768", overview.quote.sell.toPlainString())

        val order = prepare(reference)
        assertEquals(pen("374.20"), order.credit)
        val receipt = success(confirm(order))

        assertEquals(BigDecimal("3.742"), receipt.rate)
        assertEquals(usd("100.00"), receipt.debited)
        assertEquals(pen("374.20"), receipt.credited)
        assertEquals(usd("950.20"), receipt.sourceAvailableBalance)
        assertEquals(pen("2674.20"), receipt.destinationAvailableBalance)
        // El cálculo del cliente coincide con el del servidor.
        assertEquals(order.credit, receipt.credited)

        // Reintentar con la misma clave no vuelve a cambiar.
        assertEquals(receipt, success(exchanges.exchange(order)))
        assertEquals(usd("950.20"), balance("ACC-003"))
        assertEquals(pen("2674.20"), balance("ACC-002"))
        // Las cuentas se volvieron a pedir una vez, porque el cambio subió la revisión.
        assertEquals(2, count("GET accounts"))
    }

    @Test
    fun `soles to dollars divide by the sell rate exactly like the server`() = runTest {
        login()
        listOf("376.80", "10.00", "1.00", "0.02").forEach { amount ->
            val order = prepare(ExchangeDraft(ExchangeDirection.PEN_TO_USD, amount, "ACC-001", "ACC-003"))

            val receipt = success(confirm(order))

            assertEquals(order.credit, receipt.credited)
            assertEquals(BigDecimal("3.768"), receipt.rate)
        }
    }

    @Test
    fun `an expired quote is offered again, and the new one with a new key makes the exchange once`() = runTest {
        scenario.update(mapOf("quote_ttl_s" to "5"))
        login()
        val order = prepare(reference)
        assertEquals(clock.now.plusSeconds(5), order.expiresAt.toInstant())
        clock.now = clock.now.plusSeconds(5)

        val expired = (confirm(order) as Outcome.Failure).error as AppError.QuoteExpired
        val fresh = requireNotNull(expired.quote)
        assertNotEquals(order.quoteId, fresh.quoteId)
        assertEquals(clock.now.plusSeconds(5), fresh.expiresAt.toInstant())
        assertEquals(usd("1050.20"), balance("ACC-003"))

        val renewed = order.withQuote(fresh, keys.newKey())
        assertNotEquals(order.idempotencyKey, renewed.idempotencyKey)
        val receipt = success(confirm(renewed))

        assertEquals(fresh.quoteId, receipt.quoteId)
        assertEquals(usd("950.20"), balance("ACC-003"))
        assertEquals(2, count("POST fx/exchanges"))
    }

    @Test
    fun `the server does not keep a quote expired answer under its key`() = runTest {
        // Por eso una clave que recibió QUOTE_EXPIRED nunca hizo el cambio y la cotización nueva puede ir con otra.
        scenario.update(mapOf("quote_ttl_s" to "5"))
        login()
        val order = prepare(reference)
        clock.now = clock.now.plusSeconds(5)
        val fresh = requireNotNull(((confirm(order) as Outcome.Failure).error as AppError.QuoteExpired).quote)

        val sameKey = success(confirm(order.withQuote(fresh, order.idempotencyKey)))

        assertEquals(fresh.quoteId, sameKey.quoteId)
        assertEquals(usd("950.20"), balance("ACC-003"))
    }

    @Test
    fun `the tab opens the confirmation with a quote asked for at continue`() = runTest {
        login()
        val stream = FakeMarketStream()
        val viewModel = ExchangeViewModel(
            loadExchange = LoadExchangeUseCase(accounts, market),
            getAccounts = GetAccountsUseCase(accounts),
            getAccountsRevision = GetAccountsRevisionUseCase(accountChanges),
            validateExchange = validate,
            startExchange = start,
            observeFxQuotes = ObserveFxQuotesUseCase(stream),
            observeStreamStatus = ObserveMarketStreamStatusUseCase(stream)
        )
        val loaded = viewModel.uiState.first { it.contentKey == 1 }
        assertEquals("ACC-003", loaded.sourceAccountId)

        viewModel.onAction(ExchangeAction.AmountChanged("100"))
        viewModel.onAction(ExchangeAction.AccountClicked(ExchangeAccountRole.DESTINATION))
        viewModel.onAction(ExchangeAction.AccountSelected("ACC-002"))
        assertEquals(pen("374.20"), viewModel.uiState.first { it.destinationAccountId != null }.estimatedCredit)
        viewModel.onAction(ExchangeAction.ContinueClicked)
        val order = requireNotNull(viewModel.uiState.first { it.order != null }.order)

        // Una cotización en la carga y otra en Continuar: la confirmación empieza con la vigencia completa.
        assertEquals(2, count("GET fx/rates"))
        assertEquals("QTE-000002", order.quoteId)
        assertEquals(clock.now.plusSeconds(30), order.expiresAt.toInstant())
        assertEquals(pen("374.20"), success(confirm(order)).credited)
    }

    /** Lo que hace el tab al tocar Continuar: una cotización nueva y el cambio validado con ella. */
    private suspend fun prepare(draft: ExchangeDraft): ExchangeOrder {
        val own = success(GetAccountsUseCase(accounts)())
        val prepared = success(start(draft, own))
        return (prepared.validation as ExchangeValidation.Valid).order
    }

    private suspend fun login() {
        val result = caller { api.login(LoginRequestDto("ana.torres", "Cartera2026")) }
        tokens.update((result as ApiResult.Success).value.accessToken)
    }

    private suspend fun balance(accountId: String): Money =
        success(GetAccountsUseCase(accounts)()).single { it.id == accountId }.availableBalance

    private fun count(call: String) = calls[call]?.get() ?: 0

    private fun usd(value: String) = Money(value.toBigDecimal(), "USD")

    private fun pen(value: String) = Money(value.toBigDecimal(), "PEN")

    private fun <T> success(outcome: Outcome<T>): T = (outcome as Outcome.Success).value
}
