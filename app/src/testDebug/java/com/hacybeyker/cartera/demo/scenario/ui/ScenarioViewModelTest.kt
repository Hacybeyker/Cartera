package com.hacybeyker.cartera.demo.scenario.ui

import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.demo.backend.FailureMode
import com.hacybeyker.cartera.demo.scenario.DemoResetClient
import com.hacybeyker.cartera.demo.scenario.FakeScenarioStorage
import com.hacybeyker.cartera.demo.scenario.Scenario
import com.hacybeyker.cartera.demo.scenario.ScenarioStore
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ScenarioViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val server = MockWebServer()
    private val storage = FakeScenarioStorage()
    private val store = ScenarioStore(storage)

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun viewModel() = ScenarioViewModel(
        store = store,
        resetClient = DemoResetClient(OkHttpClient(), server.url("/api/v1/")),
        io = mainDispatcherRule.testDispatcher
    )

    @Test
    fun `shows the stored scenario with the adb format`() = runTest {
        store.update(mapOf("latency" to "default=100,movements=2000", "backend_down" to true))

        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("default=100,movements=2000", state.latency)
        assertEquals("default=0", state.failureRate)
        assertEquals(FailureMode.HTTP_500, state.failureMode)
        assertEquals("900", state.sessionTtl)
        assertEquals(true, state.backendDown)
        assertEquals("2000", state.tickMs)
        assertEquals("0", state.streamDrop)
        assertEquals("30", state.quoteTtl)
    }

    @Test
    fun `market fields are saved like the adb extras`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(ScenarioAction.TickMsChanged("0"))
        viewModel.onAction(ScenarioAction.StreamDropChanged("15"))
        viewModel.onAction(ScenarioAction.QuoteTtlChanged("5"))
        assertEquals(null, viewModel.uiState.value.message)
        viewModel.onAction(ScenarioAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(Scenario(tickMs = 0, streamDropSeconds = 15, quoteTtlSeconds = 5), store.load())
        assertEquals("15", viewModel.uiState.value.streamDrop)
        assertEquals(ScenarioMessage.Saved, viewModel.uiState.value.message)
    }

    @Test
    fun `invalid market values are reported`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(ScenarioAction.QuoteTtlChanged("0"))
        viewModel.onAction(ScenarioAction.TickMsChanged("rapido"))
        viewModel.onAction(ScenarioAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(
            ScenarioMessage.Rejected(listOf("tick_ms=rapido", "quote_ttl_s=0")),
            viewModel.uiState.value.message
        )
        assertEquals("30", viewModel.uiState.value.quoteTtl)
    }

    @Test
    fun `save stores every field like a broadcast`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(ScenarioAction.LatencyChanged("default=0,movements=1500"))
        viewModel.onAction(ScenarioAction.FailureRateChanged("transfers=100"))
        viewModel.onAction(ScenarioAction.FailureModeSelected(FailureMode.TIMEOUT))
        viewModel.onAction(ScenarioAction.SessionTtlChanged("60"))
        viewModel.onAction(ScenarioAction.BackendDownChanged(true))
        viewModel.onAction(ScenarioAction.SaveClicked)
        advanceUntilIdle()

        val saved = store.load()
        assertEquals(1500L, saved.latencyMs.valueFor("movements"))
        assertEquals(100, saved.failureRate.valueFor("transfers"))
        assertEquals(FailureMode.TIMEOUT, saved.failureMode)
        assertEquals(60L, saved.sessionTtlSeconds)
        assertEquals(true, saved.backendDown)
        assertEquals(ScenarioMessage.Saved, viewModel.uiState.value.message)
        assertEquals("default=0,transfers=100", viewModel.uiState.value.failureRate)
    }

    @Test
    fun `invalid values are reported and keep the previous value`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(ScenarioAction.SessionTtlChanged("0"))
        viewModel.onAction(ScenarioAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(ScenarioMessage.Rejected(listOf("session_ttl=0")), viewModel.uiState.value.message)
        assertEquals("900", viewModel.uiState.value.sessionTtl)
        assertEquals(900L, store.load().sessionTtlSeconds)
    }

    @Test
    fun `defaults restore the factory scenario`() = runTest {
        store.update(mapOf("session_ttl" to 60, "backend_down" to true))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(ScenarioAction.DefaultsClicked)
        advanceUntilIdle()

        assertEquals(Scenario(), store.load())
        assertEquals("900", viewModel.uiState.value.sessionTtl)
        assertFalse(viewModel.uiState.value.backendDown)
        assertEquals(ScenarioMessage.DefaultsRestored, viewModel.uiState.value.message)
    }

    @Test
    fun `reset data posts to the demo endpoint and reports the result`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        server.enqueue(MockResponse.Builder().code(500).build())
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(ScenarioAction.ResetDataClicked)
        advanceUntilIdle()
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/_demo/reset", request.url.encodedPath)
        assertEquals(ScenarioMessage.DataReset, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isResettingData)

        viewModel.onAction(ScenarioAction.ResetDataClicked)
        advanceUntilIdle()
        assertEquals(ScenarioMessage.DataResetFailed, viewModel.uiState.value.message)
    }

    @Test
    fun `editing a field clears the last message`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onAction(ScenarioAction.SaveClicked)
        advanceUntilIdle()

        viewModel.onAction(ScenarioAction.LatencyChanged("0"))

        assertEquals(null, viewModel.uiState.value.message)
    }

    @Test
    fun `client flags are saved with the rest`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(ScenarioAction.FlagChanged(FeatureFlag.ACCOUNTS_EAGER_REFRESH, true))
        viewModel.onAction(ScenarioAction.FlagChanged(FeatureFlag.BREAKDOWN_RICH_TRANSITION, true))
        viewModel.onAction(ScenarioAction.FlagChanged(FeatureFlag.BREAKDOWN_RICH_TRANSITION, false))
        viewModel.onAction(ScenarioAction.FlagChanged(FeatureFlag.QUOTES_FULL_REFRESH, true))
        viewModel.onAction(ScenarioAction.FlagChanged(FeatureFlag.CHART_LIVE_PATH, true))
        viewModel.onAction(ScenarioAction.SaveClicked)
        advanceUntilIdle()

        val saved = setOf(
            FeatureFlag.ACCOUNTS_EAGER_REFRESH,
            FeatureFlag.QUOTES_FULL_REFRESH,
            FeatureFlag.CHART_LIVE_PATH
        )
        assertEquals(saved, store.load().flags)
        assertEquals(saved, viewModel.uiState.value.flags)

        viewModel.onAction(ScenarioAction.DefaultsClicked)
        advanceUntilIdle()
        assertEquals(emptySet<FeatureFlag>(), viewModel.uiState.value.flags)
    }
}
