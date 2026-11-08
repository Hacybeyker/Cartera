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
import com.hacybeyker.cartera.core.data.network.dto.toMovement
import com.hacybeyker.cartera.core.data.portfolio.InMemoryPortfolioChanges
import com.hacybeyker.cartera.core.data.portfolio.RemotePortfolioRepository
import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.GetAccountsUseCase
import com.hacybeyker.cartera.core.domain.flags.IsFeatureEnabledUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveQuotesUseCase
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioRevisionUseCase
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioUseCase
import com.hacybeyker.cartera.core.testing.FakeFeatureFlags
import com.hacybeyker.cartera.core.testing.FakeMarketStream
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.SequentialKeys
import com.hacybeyker.cartera.demo.backend.CarteraBackend
import com.hacybeyker.cartera.demo.backend.MutableClock
import com.hacybeyker.cartera.demo.backend.fileFixtures
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadMarketUseCase
import com.hacybeyker.cartera.feature.home.ui.market.MarketAction
import com.hacybeyker.cartera.feature.home.ui.market.MarketViewModel
import com.hacybeyker.cartera.feature.order.data.repository.RemoteOrderRepository
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderDraft
import com.hacybeyker.cartera.feature.order.domain.model.OrderHolding
import com.hacybeyker.cartera.feature.order.domain.model.OrderValidation
import com.hacybeyker.cartera.feature.order.domain.usecase.ConfirmOrderUseCase
import com.hacybeyker.cartera.feature.order.domain.usecase.LoadOrderFormUseCase
import com.hacybeyker.cartera.feature.order.domain.usecase.ValidateOrderUseCase
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Las órdenes de referencia (`tick_ms 0`) con el cliente de verdad contra el backend embebido. */
class OrderClientEndToEndTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val backend = CarteraBackend(fileFixtures, MutableClock())
    private val scenario = ScenarioStore(FakeScenarioStorage())
    private val tokens = InMemoryAccessTokenStore()
    private val caller = ApiCaller(CarteraHttpClient.json) {}
    private val calls = ConcurrentHashMap<String, AtomicInteger>()
    private val accountChanges = InMemoryAccountChanges()
    private val portfolioChanges = InMemoryPortfolioChanges()
    private lateinit var client: OkHttpClient
    private lateinit var api: CarteraApi
    private lateinit var marketApi: MarketApi

    private val accounts by lazy { RemoteAccountRepository(api, caller, accountChanges, tokens) }
    private val portfolio by lazy { RemotePortfolioRepository(marketApi, caller, portfolioChanges, tokens) }
    private val market by lazy { RemoteMarketRepository(marketApi, caller) }
    private val loadForm by lazy { LoadOrderFormUseCase(accounts, portfolio) }
    private val validate = ValidateOrderUseCase(SequentialKeys())
    private val orders by lazy { RemoteOrderRepository(marketApi, caller) }
    private val confirm by lazy { ConfirmOrderUseCase(orders, accountChanges, portfolioChanges) }

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
    fun `the reference buy and then the reference sell`() = runTest {
        login()

        val buy = order("ANDM", OrderSide.BUY, "2")
        assertEquals(usd("84.20"), buy.estimatedTotal)
        val bought = success(confirm(buy))
        assertEquals(usd("42.10"), bought.executedPrice)
        assertEquals(usd("84.20"), bought.total)
        assertEquals(usd("966.00"), bought.accountAvailableBalance)
        assertEquals(OrderHolding(12, usd("40.35")), bought.holding)

        // Reintentar con la misma clave no vuelve a cobrar.
        assertEquals(bought, success(orders.place(buy)))
        assertEquals(usd("966.00"), dollarBalance())

        val sell = order("QORI", OrderSide.SELL, "1")
        val sold = success(confirm(sell))
        assertEquals(usd("115.20"), sold.total)
        assertEquals(usd("1081.20"), sold.accountAvailableBalance)
        assertEquals(1, sold.holding.quantity)

        val holdings = success(portfolio.portfolio()).holdings.associateBy { it.symbol }
        assertEquals(12, holdings.getValue("ANDM").quantity)
        assertEquals("40.35", holdings.getValue("ANDM").averagePrice.toPlainString())
        assertEquals(1, holdings.getValue("QORI").quantity)

        val movements = (caller { api.movements("ACC-003", cursor = null, limit = 20) } as ApiResult.Success).value
        val latest = movements.items.take(2).map { it.toMovement() }
        assertEquals(listOf(MovementCategory.INVESTMENT, MovementCategory.INVESTMENT), latest.map { it.category })
        assertEquals(usd("115.20"), latest.first().amount)
        assertEquals(usd("-84.20"), latest.last().amount)
    }

    @Test
    fun `the form stops the sell of more shares than held without calling the server`() = runTest {
        login()
        val form = success(loadForm("QORI", OrderSide.SELL))
        val price = success(market.stock("QORI")).quote

        val validation = validate(OrderDraft("QORI", OrderSide.SELL, "3", "ACC-003", Money(price.last, "USD")), form)

        assertEquals(OrderValidation.Invalid::class, validation::class)
        assertEquals(0, count("POST orders"))
    }

    @Test
    fun `back to market after an order asks for the portfolio once more and never for the stocks`() = runTest {
        login()
        val stream = FakeMarketStream()
        val viewModel = MarketViewModel(
            loadMarket = LoadMarketUseCase(market, portfolio),
            getPortfolio = GetPortfolioUseCase(portfolio),
            getPortfolioRevision = GetPortfolioRevisionUseCase(portfolioChanges),
            observeQuotes = ObserveQuotesUseCase(stream),
            observeStreamStatus = ObserveMarketStreamStatusUseCase(stream),
            isFeatureEnabled = IsFeatureEnabledUseCase(FakeFeatureFlags())
        )
        viewModel.uiState.first { it.contentKey == 1 }
        assertEquals(usd("1094.40"), viewModel.uiState.value.portfolio?.marketValue)
        success(GetAccountsUseCase(accounts)())

        // El formulario usa las cachés: ni cuentas ni portafolio de nuevo.
        success(confirm(order("ANDM", OrderSide.BUY, "2")))
        assertEquals(1, count("GET accounts"))
        assertEquals(1, count("GET portfolio"))

        // Volver a Mercado: el tab se muestra otra vez, dos veces incluso (la transición y el frame final).
        viewModel.onAction(MarketAction.Shown)
        viewModel.onAction(MarketAction.Shown)
        viewModel.uiState.first { it.portfolio?.marketValue == usd("1178.60") }

        assertEquals(2, count("GET portfolio"))
        assertEquals(1, count("GET market/stocks"))
        // Inicio vuelve a pedir las cuentas: el saldo cambió.
        assertEquals(usd("966.00"), dollarBalance())
        assertEquals(2, count("GET accounts"))
    }

    /** Lo que hace el formulario al tocar Continuar, con el precio vigente del servidor como precio en vivo. */
    private suspend fun order(symbol: String, side: OrderSide, quantity: String): MarketOrder {
        val form = success(loadForm(symbol, side))
        val price = success(market.stock(symbol)).quote
        val draft = OrderDraft(symbol, side, quantity, form.accounts.single().id, Money(price.last, price.currency))
        return (validate(draft, form) as OrderValidation.Valid).order
    }

    private suspend fun login() {
        val result = caller { api.login(LoginRequestDto("ana.torres", "Cartera2026")) }
        tokens.update((result as ApiResult.Success).value.accessToken)
    }

    private suspend fun dollarBalance(): Money =
        success(GetAccountsUseCase(accounts)()).single { it.id == "ACC-003" }.availableBalance

    private fun count(call: String) = calls[call]?.get() ?: 0

    private fun usd(value: String) = Money(value.toBigDecimal(), "USD")

    private fun <T> success(outcome: Outcome<T>): T = (outcome as Outcome.Success).value
}
