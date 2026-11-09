package com.hacybeyker.cartera.legacy.debts

import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.core.data.LegacyJson
import com.hacybeyker.cartera.legacy.core.di.LegacyNetworkModule
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.debts.data.JsonDebtsBodyDecoder
import com.hacybeyker.cartera.legacy.debts.data.RemoteDebtRepository
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import com.hacybeyker.cartera.legacy.testing.FakeLegacyFeatureFlags
import com.hacybeyker.cartera.legacy.testing.IO_THREAD
import com.hacybeyker.cartera.legacy.testing.MAIN_THREAD
import com.hacybeyker.cartera.legacy.testing.NamedThreadSchedulers
import com.hacybeyker.cartera.legacy.testing.RecordingDebtsBodyDecoder
import com.hacybeyker.cartera.legacy.testing.RecordingMarker
import com.hacybeyker.cartera.legacy.testing.RecordingSessionExpiry
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.ThreadRecordingInterceptor
import com.hacybeyker.cartera.legacy.testing.luzBodyWithHistory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DebtsLoadingPathsTest {
    private val server = MockWebServer()
    private val schedulers = NamedThreadSchedulers()
    private val interceptor = ThreadRecordingInterceptor()
    private val flags = FakeLegacyFeatureFlags()
    private val decoder = RecordingDebtsBodyDecoder(JsonDebtsBodyDecoder(LegacyJson.instance))
    private val marker = RecordingMarker()
    private val flow = PaymentFlow().apply {
        chooseBiller(Samples.luz)
        chooseCustomerCode("100200")
    }
    private val presenters = mutableListOf<DebtsPresenter>()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        presenters.forEach(DebtsPresenter::destroy)
        schedulers.close()
        server.close()
    }

    @Test
    fun `with the flag off the debts load through the first path`() {
        server.enqueue(json(PLAIN_BODY))

        val view = load(newPresenter())

        assertEquals(null, server.takeRequest().url.queryParameter("includeHistory"))
        assertOnly(IO_THREAD, interceptor.callThreads)
        assertOnly(IO_THREAD, interceptor.bodyThreads)
        assertEquals(emptyList<String>(), decoder.threads)
        assertEquals(MAIN_THREAD, view.renderThreads.last())
        assertShowsTheLuzDebts(view)
    }

    @Test
    fun `with the flag on the debts load through the second path`() {
        flags.debtsMainDecode = true
        server.enqueue(json(luzBodyWithHistory()))

        val view = load(newPresenter())

        assertEquals("true", server.takeRequest().url.queryParameter("includeHistory"))
        assertOnly(IO_THREAD, interceptor.callThreads)
        assertOnly(IO_THREAD, interceptor.bodyThreads)
        assertEquals(listOf(MAIN_THREAD), decoder.threads)
        assertEquals(MAIN_THREAD, view.renderThreads.last())
        assertShowsTheLuzDebts(view)
    }

    @Test
    fun `both paths show the same debts and each reports the mark once`() {
        server.enqueue(json(PLAIN_BODY))
        server.enqueue(json(luzBodyWithHistory()))

        val plain = load(newPresenter())
        flags.debtsMainDecode = true
        val withHistory = load(newPresenter())

        assertEquals(plain.states.last(), withHistory.states.last())
        assertEquals(listOf("legacy_recibos", "legacy_recibos"), marker.marks)
    }

    private fun newPresenter(): DebtsPresenter {
        val retrofit = LegacyNetworkModule.provideRetrofit(
            LegacyHttpClient { OkHttpClient.Builder().addInterceptor(interceptor).build() },
            object : LegacyApiConfig {
                override val baseUrl: String = server.url("/api/v1/").toString()
            }
        )
        val repository = RemoteDebtRepository(
            LegacyNetworkModule.provideDebtsApi(retrofit),
            LegacyApiCaller(LegacyJson.instance, RecordingSessionExpiry()),
            flags,
            schedulers,
            decoder
        )
        return DebtsPresenter(repository, flow, marker, schedulers).also(presenters::add)
    }

    private fun load(presenter: DebtsPresenter): LoadedDebtsView {
        val view = LoadedDebtsView()
        val marksBefore = marker.marks.size
        presenter.attach(view)
        assertTrue("The debts never loaded", view.loaded.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        while (marker.marks.size == marksBefore && System.nanoTime() < deadline) Thread.sleep(POLL_MILLIS)
        return view
    }

    private fun assertOnly(thread: String, threads: List<String>) {
        assertTrue(threads.isNotEmpty())
        assertEquals(setOf(thread), threads.toSet())
    }

    private fun assertShowsTheLuzDebts(view: LoadedDebtsView) {
        val state = view.states.last()
        assertEquals(LoadPhase.CONTENT, state.phase)
        assertEquals("Ana Torres", state.customerName)
        assertEquals(listOf("S/ 85.40", "S/ 92.10"), state.items.map { it.amountLabel })
        assertEquals(listOf("legacy_recibos"), marker.marks)
    }

    private fun json(body: String) =
        MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build()

    private class LoadedDebtsView : DebtsView {
        val states = CopyOnWriteArrayList<DebtsViewState>()
        val renderThreads = CopyOnWriteArrayList<String>()
        val loaded = CountDownLatch(1)

        override fun render(state: DebtsViewState) {
            states += state
            renderThreads += Thread.currentThread().name
            if (state.phase == LoadPhase.CONTENT) loaded.countDown()
        }

        override fun showAccount() = Unit

        override fun returnToCustomerCode() = Unit

        override fun goBack() = Unit

        override fun close() = Unit
    }

    private companion object {
        const val TIMEOUT_SECONDS = 10L
        const val POLL_MILLIS = 5L
        const val PLAIN_BODY =
            """{"customerName":"Ana Torres","debts":[""" +
                """{"id":"DEB-LUZ-202610","period":"2026-10","dueDate":"2026-11-12",""" +
                """"amount":{"value":"85.40","currency":"PEN"}},""" +
                """{"id":"DEB-LUZ-202611","period":"2026-11","dueDate":"2026-12-12",""" +
                """"amount":{"value":"92.10","currency":"PEN"}}]}"""
    }
}
