package com.hacybeyker.cartera.legacy.ui

import android.os.SystemClock
import android.view.MotionEvent
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.account.AccountPresenter
import com.hacybeyker.cartera.legacy.billers.BillersPresenter
import com.hacybeyker.cartera.legacy.billers.SEARCH_DEBOUNCE_MILLIS
import com.hacybeyker.cartera.legacy.billers.data.RemoteBillerRepository
import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.core.data.LegacyJson
import com.hacybeyker.cartera.legacy.core.di.LegacyNetworkModule
import com.hacybeyker.cartera.legacy.customer.CustomerCodePresenter
import com.hacybeyker.cartera.legacy.debts.DebtsPresenter
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import com.hacybeyker.cartera.legacy.payment.data.RemotePaymentRepository
import com.hacybeyker.cartera.legacy.pin.PinPresenter
import com.hacybeyker.cartera.legacy.pin.domain.PinKey
import com.hacybeyker.cartera.legacy.testing.FakeLegacyFeatureFlags
import com.hacybeyker.cartera.legacy.testing.FakePinPadSeed
import com.hacybeyker.cartera.legacy.testing.ImmediateSchedulerProvider
import com.hacybeyker.cartera.legacy.testing.RecordingMarker
import com.hacybeyker.cartera.legacy.testing.RecordingSessionExpiry
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.SequenceIdempotencyKeys
import com.hacybeyker.cartera.legacy.ui.pinpad.PinPadView
import com.hacybeyker.cartera.legacy.voucher.VoucherPresenter
import java.util.concurrent.TimeUnit
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/**
 * El camino completo de un 401 dentro de `:legacy`, sin dobles entre la red y la pantalla: Retrofit sobre un servidor
 * real, el traductor de errores, el repositorio, el Presenter y el Fragment. Quien aloja el módulo recibe el aviso de
 * sesión vencida una vez y el módulo pide cerrarse sin mostrar un diálogo ni marcar el pago como posible.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class SessionExpiryChainTest {
    private val server = MockWebServer()
    private val expiry = RecordingSessionExpiry()
    private val schedulers = ImmediateSchedulerProvider()
    private val flow = PaymentFlow().apply { start() }
    private lateinit var presenters: LegacyPresenterSource

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()
        val retrofit = LegacyNetworkModule.provideRetrofit(
            LegacyHttpClient { client },
            object : LegacyApiConfig {
                override val baseUrl: String = server.url("/api/v1/").toString()
            }
        )
        val caller = LegacyApiCaller(LegacyJson.instance, expiry)
        val billers = RemoteBillerRepository(LegacyNetworkModule.provideBillersApi(retrofit), caller)
        val payments = RemotePaymentRepository(LegacyNetworkModule.providePaymentsApi(retrofit), caller)
        presenters = object : LegacyPresenterSource {
            override fun billers() =
                BillersPresenter(billers, flow, RecordingMarker(), FakeLegacyFeatureFlags(), schedulers)

            override fun pin() = PinPresenter(
                payments,
                flow,
                FakePinPadSeed(),
                SequenceIdempotencyKeys(),
                schedulers
            )

            override fun customerCode(): CustomerCodePresenter = error("no se usa")

            override fun debts(): DebtsPresenter = error("no se usa")

            override fun account(): AccountPresenter = error("no se usa")

            override fun voucher(): VoucherPresenter = error("no se usa")
        }
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `a 401 on the search of billers closes the module once and reports the expiry once, without a dialog`() {
        server.enqueue(json(200, """{"items":[]}"""))
        server.enqueue(problem401())
        val activity = launchScreen(presenters) { BillersFragment() }.get()

        activity.search("a")

        assertEquals(1, HostEnvironment.closes)
        assertEquals(1, expiry.calls)
        assertNull(ShadowDialog.getLatestDialog())
        assertFalse(flow.accountsChanged)
        assertEquals(listOf("/api/v1/billers?query=", "/api/v1/billers?query=a"), requestedPaths())
    }

    @Test
    fun `a 401 without a problem body closes the module the same way`() {
        server.enqueue(json(200, """{"items":[]}"""))
        server.enqueue(MockResponse.Builder().code(401).build())
        val activity = launchScreen(presenters) { BillersFragment() }.get()

        activity.search("agua")

        assertEquals(1, HostEnvironment.closes)
        assertEquals(1, expiry.calls)
        assertNull(ShadowDialog.getLatestDialog())
    }

    @Test
    fun `a 401 on the first load of billers closes the module before anything is typed`() {
        server.enqueue(problem401())

        launchScreen(presenters) { BillersFragment() }

        assertEquals(1, HostEnvironment.closes)
        assertEquals(1, expiry.calls)
        assertEquals(listOf("/api/v1/billers?query="), requestedPaths())
    }

    @Test
    fun `a 401 on the payment closes the module without flagging a possible charge`() {
        flow.chooseBiller(Samples.luz)
        flow.chooseCustomerCode("100200")
        flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        flow.chooseAccount(Samples.gastos)
        server.enqueue(problem401())
        val activity = launchScreen(presenters) { PinFragment() }.get()

        activity.press(1, 2, 3, 4)

        assertEquals(1, HostEnvironment.closes)
        assertEquals(1, expiry.calls)
        assertNull(ShadowDialog.getLatestDialog())
        assertFalse(flow.accountsChanged)
        assertEquals(1, server.requestCount)
    }

    private fun LegacyHostActivity.search(text: String) {
        findViewById<TextView>(R.id.legacy_empresas_buscar).text = text
        schedulers.clock.advanceTimeBy(SEARCH_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
        idle()
    }

    private fun LegacyHostActivity.press(vararg digits: Int) {
        val pad = window.decorView.findViewWithTag<PinPadView>("legacy_pin_pad")
        digits.forEach { digit ->
            val bounds = pad.boundsOf(pad.slots.first { it.key == PinKey.Digit(digit) })
            val down = SystemClock.uptimeMillis()
            pad.dispatchTouchEvent(
                MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, bounds.exactCenterX(), bounds.exactCenterY(), 0)
            )
            pad.dispatchTouchEvent(
                MotionEvent.obtain(
                    down,
                    down + 30,
                    MotionEvent.ACTION_UP,
                    bounds.exactCenterX(),
                    bounds.exactCenterY(),
                    0
                )
            )
        }
        idle()
    }

    private fun requestedPaths(): List<String> = (1..server.requestCount).map { server.takeRequest().target }

    private fun json(code: Int, body: String) = MockResponse.Builder().code(code).body(body)
        .addHeader("Content-Type", "application/json").build()

    private fun problem401() = MockResponse.Builder().code(401).body(
        """{"type":"https://cartera.invalid/problems/session-expired","title":"Sesión expirada","status":401,""" +
            """"detail":"Tu sesión expiró","code":"SESSION_EXPIRED","traceId":"req-7"}"""
    ).addHeader("Content-Type", "application/problem+json").build()
}
