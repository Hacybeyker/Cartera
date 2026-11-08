package com.hacybeyker.cartera.feature.transfer.ui

import com.hacybeyker.cartera.core.data.account.InMemoryAccountChanges
import com.hacybeyker.cartera.core.data.account.RemoteAccountRepository
import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.account.GetAccountsRevisionUseCase
import com.hacybeyker.cartera.core.domain.account.GetAccountsUseCase
import com.hacybeyker.cartera.core.domain.account.RefreshAccountsUseCase
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.testing.FakeFeatureFlags
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.SequentialKeys
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.pen
import com.hacybeyker.cartera.feature.home.data.repository.RemoteProfileRepository
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadOverviewUseCase
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewAction
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewViewModel
import com.hacybeyker.cartera.feature.transfer.data.repository.RemoteTransferRepository
import com.hacybeyker.cartera.feature.transfer.domain.usecase.ConfirmTransferUseCase
import com.hacybeyker.cartera.feature.transfer.domain.usecase.ValidateTransferUseCase
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmAction
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmExit
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmViewModel
import com.hacybeyker.cartera.feature.transfer.ui.form.TransferFormAction
import com.hacybeyker.cartera.feature.transfer.ui.form.TransferFormViewModel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** El recorrido de la transferencia de referencia contra el mismo servidor, contando cada llamada por ruta. */
class BackToHomeAfterTransferTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val server = MockWebServer()
    private val calls = ConcurrentHashMap<String, AtomicInteger>()
    private var balance = "2300.00"

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath.removePrefix("/api/v1/")
                calls.getOrPut(path) { AtomicInteger() }.incrementAndGet()
                return when (path) {
                    "me" -> json(200, ME)
                    "accounts" -> json(200, """{"items":[${account(balance)}]}""")
                    "transfers" -> json(201, RECEIPT).also { balance = "2150.00" }
                    else -> json(404, "{}")
                }
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.close()

    private fun count(path: String) = calls[path]?.get() ?: 0

    @Test
    fun `back to home after the reference transfer asks for the accounts once and never for me`() = runTest {
        transferAndComeBack(FakeFeatureFlags())

        // Una al abrir Inicio y una al volver; el formulario usó la caché.
        assertEquals(2, count("accounts"))
        assertEquals(1, count("me"))
        assertEquals(1, count("transfers"))
    }

    @Test
    fun `with the eager refresh, back to home asks for the accounts twice more`() = runTest {
        transferAndComeBack(FakeFeatureFlags(FeatureFlag.ACCOUNTS_EAGER_REFRESH))

        assertEquals(3, count("accounts"))
        assertEquals(1, count("me"))
    }

    private suspend fun transferAndComeBack(flags: FakeFeatureFlags) {
        val api = server.carteraApi()
        val call = ApiCaller(CarteraHttpClient.json) {}
        val changes = InMemoryAccountChanges()
        val accounts = RemoteAccountRepository(api, call, changes) { "token" }
        val revision = GetAccountsRevisionUseCase(changes)
        val overview = OverviewViewModel(
            LoadOverviewUseCase(RemoteProfileRepository(api, call), accounts),
            RefreshAccountsUseCase(accounts, flags),
            revision
        )
        overview.uiState.first { it.contentKey == 1 }

        val form = TransferFormViewModel(GetAccountsUseCase(accounts), ValidateTransferUseCase(SequentialKeys()))
        form.uiState.first { !it.isLoading }
        // El formulario usa la caché: ninguna llamada nueva.
        assertEquals(1, count("accounts"))
        form.onAction(TransferFormAction.SourceSelected("ACC-002"))
        form.onAction(TransferFormAction.DestinationChanged("999-0000001-0-01"))
        form.onAction(TransferFormAction.AmountChanged("150.00"))
        form.onAction(TransferFormAction.ContinueClicked)
        val order = requireNotNull(form.uiState.value.order)

        val confirm =
            TransferConfirmViewModel(order, ConfirmTransferUseCase(RemoteTransferRepository(api, call), changes))
        confirm.onAction(TransferConfirmAction.ConfirmClicked)
        val exit = confirm.uiState.first { it.exit != null }.exit as TransferConfirmExit.Done
        assertEquals(pen("2150.00"), exit.receipt.sourceAvailableBalance)

        // Volver al inicio: Inicio se muestra otra vez, dos veces incluso (la transición y el frame final).
        overview.onAction(OverviewAction.Shown)
        overview.onAction(OverviewAction.Shown)
        val refreshed = overview.uiState.first { it.contentKey == 2 }
        assertEquals(pen("2150.00"), refreshed.accounts.single().availableBalance)
    }

    private fun json(code: Int, body: String) =
        MockResponse.Builder().code(code).setHeader("Content-Type", "application/json").body(body).build()

    private fun account(available: String) = """{"id":"ACC-002","alias":"Cuenta Gastos","type":"CHECKING",
        "currency":"PEN","numberMasked":"****9034","availableBalance":{"value":"$available","currency":"PEN"},
        "accountingBalance":{"value":"$available","currency":"PEN"}}"""

    private companion object {
        const val ME = """{"id":"USR-001","fullName":"Ana Torres","documentMasked":"*****678","email":"a@b.c"}"""
        const val RECEIPT = """{"transferId":"TRF-000001","operationNumber":"90000001",
            "createdAt":"2026-11-07T10:00:00-05:00","sourceAccountId":"ACC-002",
            "sourceAvailableBalance":{"value":"2150.00","currency":"PEN"},"movementId":"MOV-002-T0001"}"""
    }
}
