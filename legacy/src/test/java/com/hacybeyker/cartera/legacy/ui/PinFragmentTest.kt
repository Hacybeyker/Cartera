package com.hacybeyker.cartera.legacy.ui

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.appbar.MaterialToolbar
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.pin.domain.PinPadOrder
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.failure
import com.hacybeyker.cartera.legacy.ui.pinpad.PinDotsView
import com.hacybeyker.cartera.legacy.ui.pinpad.PinPadView
import io.reactivex.rxjava3.core.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class PinFragmentTest {
    private val presenters = TestPresenters().apply {
        flow.chooseBiller(Samples.luz)
        flow.chooseCustomerCode("100200")
        flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        flow.chooseAccount(Samples.gastos)
        payments.respondPay = { Single.just(Samples.payment) }
    }

    private fun launch() = launchScreen(presenters) { PinFragment() }

    private fun LegacyHostActivity.pad(): PinPadView = window.decorView.findViewWithTag("legacy_pin_pad")

    private fun LegacyHostActivity.press(vararg digits: Int) {
        val pad = pad()
        digits.forEach { digit ->
            val slot = pad.slots.first { it.key == com.hacybeyker.cartera.legacy.pin.domain.PinKey.Digit(digit) }
            val bounds = pad.boundsOf(slot)
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

    private fun LegacyHostActivity.dots() = findViewById<PinDotsView>(R.id.legacy_clave_puntos)

    private fun LegacyHostActivity.message() = findViewById<TextView>(R.id.legacy_clave_mensaje)

    private fun rejection(code: ProblemCode) = failure(LegacyError.Rejected(422, code, "", emptyList()))

    @Test
    fun `it shows the title, four empty dots, no message and the keyboard`() {
        val activity = launch().get()

        assertEquals("Clave", activity.findViewById<MaterialToolbar>(R.id.legacy_actividad_barra).title)
        assertEquals(
            "Ingresa tu clave de operaciones",
            activity.findViewById<TextView>(R.id.legacy_clave_titulo).text.toString()
        )
        assertEquals(0, activity.dots().filled)
        assertEquals(View.INVISIBLE, activity.message().visibility)
        assertEquals("", activity.message().text.toString())
        assertEquals(View.GONE, activity.findViewById<View>(R.id.legacy_clave_cargando).visibility)
        assertEquals(PinPadOrder.FIXED, activity.pad().order)
    }

    @Test
    fun `the keyboard and the dots show no digit of the pin as text`() {
        val activity = launch().get()

        activity.press(1, 2)

        assertEquals(2, activity.dots().filled)
        assertNull(activity.dots().contentDescription?.toString()?.firstOrNull { it.isDigit() })
        assertEquals(View.NO_ID, activity.pad().id)
    }

    @Test
    fun `typing four digits sends the payment by itself`() {
        val activity = launch().get()

        activity.press(1, 2, 3)
        assertEquals(emptyList<Any>(), presenters.payments.orders)
        activity.press(4)

        assertEquals("1234", presenters.payments.orders.single().pin)
        assertEquals(listOf(LegacyScreen.VOUCHER), HostEnvironment.stackReplacedWith)
        assertEquals(Samples.payment, presenters.flow.payment)
    }

    @Test
    fun `the keyboard follows the seed of the scenario`() {
        presenters.padSeed.seed = 7

        val activity = launch().get()

        assertEquals(PinPadOrder.forSeed(7), activity.pad().order)
    }

    @Test
    fun `while sending the progress shows, the keyboard is disabled and back is ignored`() {
        presenters.payments.respondPay = { Single.never() }
        val activity = launch().get()

        activity.press(1, 2, 3, 4)
        activity.onBackPressedDispatcher.onBackPressed()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_clave_cargando).visibility)
        assertFalse(activity.pad().isEnabled)
        assertFalse(activity.isFinishing)
        assertEquals(4, activity.dots().filled)
    }

    @Test
    fun `a wrong pin shows the message, empties the dots and reorders the keyboard`() {
        presenters.padSeed.seed = 7
        presenters.payments.respondPay = { Single.error(rejection(ProblemCode.INVALID_PIN)) }
        val activity = launch().get()

        activity.press(0, 0, 0, 0)

        assertEquals(View.VISIBLE, activity.message().visibility)
        assertEquals("La clave es incorrecta. Inténtalo de nuevo.", activity.message().text.toString())
        assertEquals(0, activity.dots().filled)
        assertEquals(PinPadOrder.forSeed(8), activity.pad().order)
        assertTrue(activity.pad().isEnabled)
        assertEquals(emptyList<LegacyScreen>(), HostEnvironment.stackReplacedWith)
        assertEquals(0, HostEnvironment.backs)
    }

    @Test
    fun `not enough balance shows a dialog and accepting goes back to the account`() {
        presenters.payments.respondPay = { Single.error(rejection(ProblemCode.INSUFFICIENT_FUNDS)) }
        val activity = launch().get()

        activity.press(1, 2, 3, 4)

        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertEquals(
            "El saldo de la cuenta no alcanza para pagar el total.",
            dialog.findViewById<TextView>(android.R.id.message)?.text.toString()
        )
        assertEquals("Aceptar", dialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()
        assertEquals(1, HostEnvironment.backs)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `a debt already paid shows a dialog and accepting reloads the debts`() {
        presenters.payments.respondPay = { Single.error(rejection(ProblemCode.DEBT_ALREADY_PAID)) }
        val activity = launch().get()

        activity.press(1, 2, 3, 4)

        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertEquals(
            "Uno de los recibos ya fue pagado.",
            dialog.findViewById<TextView>(android.R.id.message)?.text.toString()
        )
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()
        assertEquals(listOf(LegacyScreen.DEBTS), HostEnvironment.restartedAt)
        assertEquals(0, HostEnvironment.backs)
    }

    @Test
    fun `any other rejection shows the generic dialog`() {
        presenters.payments.respondPay = { Single.error(rejection(ProblemCode.CURRENCY_MISMATCH)) }
        val activity = launch().get()

        activity.press(1, 2, 3, 4)

        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertTrue(dialog.isShowing)
        assertEquals(
            "No pudimos procesar el pago. Revisa los datos e inténtalo de nuevo.",
            dialog.findViewById<TextView>(android.R.id.message)?.text.toString()
        )
    }

    @Test
    fun `a network error shows the retry dialog and retrying sends the same payment`() {
        presenters.payments.respondPay = { Single.error(failure(LegacyError.Network)) }
        val activity = launch().get()
        activity.press(1, 2, 3, 4)
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertEquals("Reintentar", dialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())
        assertEquals("Cancelar", dialog.getButton(AlertDialog.BUTTON_NEGATIVE).text.toString())

        presenters.payments.respondPay = { Single.just(Samples.payment) }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()

        assertEquals(2, presenters.payments.orders.size)
        assertEquals(presenters.payments.orders[0], presenters.payments.orders[1])
        assertEquals(1, presenters.idempotencyKeys.created)
        assertEquals(listOf(LegacyScreen.VOUCHER), HostEnvironment.stackReplacedWith)
    }

    @Test
    fun `cancelling the retry dialog goes back to the account without paying`() {
        presenters.payments.respondPay = { Single.error(failure(LegacyError.Server)) }
        val activity = launch().get()
        activity.press(1, 2, 3, 4)

        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        idle()

        assertEquals(1, HostEnvironment.backs)
        assertNull(presenters.flow.payment)
        assertEquals(0, activity.dots().filled)
    }

    @Test
    fun `an expired session closes the module`() {
        presenters.payments.respondPay = { Single.error(failure(LegacyError.SessionExpired)) }
        val activity = launch().get()

        activity.press(1, 2, 3, 4)

        assertEquals(1, HostEnvironment.closes)
    }

    @Test
    fun `back is not blocked when nothing is in flight`() {
        val activity = launch().get()

        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(activity.isFinishing)
    }
}
