package com.hacybeyker.cartera.legacy.ui

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.pin.domain.PinKey
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.failure
import com.hacybeyker.cartera.legacy.ui.pinpad.PinDotsView
import com.hacybeyker.cartera.legacy.ui.pinpad.PinPadView
import io.reactivex.rxjava3.core.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/**
 * La Activity se recrea de verdad (estado guardado y restaurado, Fragments nuevos) y lo que la persona ya hizo en cada
 * pantalla tiene que seguir ahí, sin volver a pedir nada al servidor.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class RecreationStateTest {
    private val presenters = TestPresenters().apply {
        flow.chooseBiller(Samples.luz)
        flow.chooseCustomerCode("100200")
        debts.respond = { Single.just(Samples.luzDebts) }
        accounts.respond = { Single.just(listOf(Samples.sueldo, Samples.gastos)) }
        payments.respondVoucher = { Single.just(Samples.voucherHtml) }
    }

    private fun ActivityController<LegacyHostActivity>.recreated(): LegacyHostActivity {
        val activity = recreate().get()
        idle()
        layoutNow(activity.window.decorView)
        return activity
    }

    @Test
    fun `the debts keep the checked boxes and the total after the activity is recreated, without asking again`() {
        var built = 0
        presenters.onDebtsPresenter = { built++ }
        val controller = launchScreen(presenters) { DebtsFragment() }
        controller.get().debtRows()[0].performClick()
        idle()
        controller.get().debtRows()[1].performClick()
        idle()

        val activity = controller.recreated()

        assertEquals(
            listOf(true, true),
            activity.debtRows().map { it.findViewById<CheckBox>(R.id.legacy_recibos_casilla).isChecked }
        )
        assertEquals("S/ 177.50", activity.findViewById<TextView>(R.id.legacy_recibos_total).text.toString())
        assertTrue(activity.findViewById<Button>(R.id.legacy_recibos_continuar).isEnabled)
        assertEquals(1, built)
        assertEquals(1, presenters.debts.requests.size)
    }

    @Test
    fun `the debts that were left unchecked stay unchecked after the activity is recreated`() {
        val controller = launchScreen(presenters) { DebtsFragment() }
        controller.get().debtRows()[1].performClick()
        idle()

        val activity = controller.recreated()

        assertEquals(
            listOf(false, true),
            activity.debtRows().map { it.findViewById<CheckBox>(R.id.legacy_recibos_casilla).isChecked }
        )
        assertEquals("S/ 92.10", activity.findViewById<TextView>(R.id.legacy_recibos_total).text.toString())
    }

    @Test
    fun `the debts carry the recreated selection into the flow when continuing`() {
        val controller = launchScreen(presenters) { DebtsFragment() }
        controller.get().debtRows()[1].performClick()
        idle()

        val activity = controller.recreated()
        activity.findViewById<Button>(R.id.legacy_recibos_continuar).performClick()
        idle()

        assertEquals(listOf(Samples.noviembre), presenters.flow.debts)
        assertEquals(listOf(LegacyScreen.ACCOUNT), HostEnvironment.navigated)
    }

    @Test
    fun `the account keeps the chosen radio and the enabled continue after the activity is recreated`() {
        presenters.flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        var built = 0
        presenters.onAccountPresenter = { built++ }
        val controller = launchScreen(presenters) { AccountFragment() }
        controller.get().findViewById<RadioGroup>(R.id.legacy_cuenta_grupo).getChildAt(1).performClick()
        idle()

        val activity = controller.recreated()

        assertEquals(listOf(false, true), activity.radios().map { it.isChecked })
        assertTrue(activity.findViewById<Button>(R.id.legacy_cuenta_continuar).isEnabled)
        assertEquals("S/ 177.50", activity.findViewById<TextView>(R.id.legacy_cuenta_total).text.toString())
        assertEquals(2, activity.findViewById<ViewGroup>(R.id.legacy_cuenta_resumen).childCount)
        assertEquals(1, built)
        assertEquals(1, presenters.accounts.requests)
    }

    @Test
    fun `the account keeps the insufficient funds notice after the activity is recreated`() {
        presenters.flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        presenters.accounts.respond = { Single.just(listOf(Samples.poca, Samples.sueldo)) }
        val controller = launchScreen(presenters) { AccountFragment() }
        controller.get().findViewById<RadioGroup>(R.id.legacy_cuenta_grupo).getChildAt(0).performClick()
        idle()

        val activity = controller.recreated()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_cuenta_aviso).visibility)
        assertTrue(activity.findViewById<Button>(R.id.legacy_cuenta_continuar).isEnabled)
    }

    @Test
    fun `the account hands the recreated choice to the flow when continuing`() {
        presenters.flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        val controller = launchScreen(presenters) { AccountFragment() }
        controller.get().findViewById<RadioGroup>(R.id.legacy_cuenta_grupo).getChildAt(1).performClick()
        idle()

        controller.recreated().findViewById<Button>(R.id.legacy_cuenta_continuar).performClick()
        idle()

        assertEquals(Samples.gastos, presenters.flow.account)
        assertEquals(listOf(LegacyScreen.PIN), HostEnvironment.navigated)
    }

    @Test
    fun `the pin keeps the typed digits and the keyboard order, and the payment goes out once it is full`() {
        prepareForPin()
        presenters.padSeed.seed = 7
        val controller = launchScreen(presenters) { PinFragment() }
        val orderBefore = controller.get().pad().order
        controller.get().press(1, 2)

        val activity = controller.recreated()

        assertEquals(2, activity.dots().filled)
        assertEquals(orderBefore, activity.pad().order)
        assertTrue(presenters.payments.orders.isEmpty())
        activity.press(3, 4)
        assertEquals("1234", presenters.payments.orders.single().pin)
    }

    @Test
    fun `the pin keeps the invalid pin message and the reordered keyboard after the activity is recreated`() {
        prepareForPin()
        presenters.padSeed.seed = 7
        presenters.payments.respondPay = {
            Single.error(failure(LegacyError.Rejected(422, ProblemCode.INVALID_PIN, "", emptyList())))
        }
        val controller = launchScreen(presenters) { PinFragment() }
        val orderBefore = controller.get().pad().order
        controller.get().press(1, 2, 3, 4)
        val orderAfterRejection = controller.get().pad().order
        assertNotEquals(orderBefore, orderAfterRejection)

        val activity = controller.recreated()

        assertEquals(View.VISIBLE, activity.message().visibility)
        assertEquals(0, activity.dots().filled)
        assertEquals(orderAfterRejection, activity.pad().order)
        assertEquals(1, presenters.payments.orders.size)
    }

    @Test
    fun `the pin shows the progress and does not send the payment again when recreated while it is in flight`() {
        prepareForPin()
        presenters.payments.respondPay = { Single.never() }
        val controller = launchScreen(presenters) { PinFragment() }
        controller.get().press(1, 2, 3, 4)

        val activity = controller.recreated()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_clave_cargando).visibility)
        assertFalse(activity.pad().isEnabled)
        assertEquals(1, presenters.payments.orders.size)
    }

    @Test
    fun `the pin keeps the retry dialog and retries the same order and key after the activity is recreated`() {
        prepareForPin()
        presenters.payments.respondPay = { Single.error(failure(LegacyError.Server)) }
        val controller = launchScreen(presenters) { PinFragment() }
        controller.get().press(1, 2, 3, 4)
        val failed = presenters.payments.orders.single()

        controller.recreated()
        presenters.payments.respondPay = { Single.just(Samples.payment) }
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertTrue(dialog.isShowing)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()

        assertEquals(2, presenters.payments.orders.size)
        assertEquals(failed, presenters.payments.orders.last())
        assertEquals(listOf(LegacyScreen.VOUCHER), HostEnvironment.stackReplacedWith)
    }

    @Test
    fun `the voucher shows the same page again after the activity is recreated, without asking again`() {
        presenters.flow.completePayment(Samples.payment)
        val controller = launchScreen(presenters) { VoucherFragment() }
        val before = controller.get().web()
        shadowOf(before).webViewClient.onPageFinished(before, "about:blank")

        val activity = controller.recreated()

        val web = activity.web()
        assertEquals(Samples.voucherHtml, shadowOf(web).lastLoadDataWithBaseURL.data)
        assertEquals(View.VISIBLE, web.visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.legacy_constancia_cargando).visibility)
        assertEquals(listOf("PAY-000001"), presenters.payments.vouchers)
        shadowOf(web).webViewClient.onPageFinished(web, "about:blank")
        assertEquals(listOf("legacy_constancia"), presenters.marker.marks)
    }

    @Test
    fun `the voucher finish button still closes the module after the activity is recreated`() {
        presenters.flow.completePayment(Samples.payment)
        val controller = launchScreen(presenters) { VoucherFragment() }

        controller.recreated().findViewById<Button>(R.id.legacy_constancia_finalizar).performClick()

        assertEquals(1, HostEnvironment.closes)
    }

    @Test
    fun `the voucher keeps its failure message and the finish button after the activity is recreated`() {
        presenters.flow.completePayment(Samples.payment)
        presenters.payments.respondVoucher = { Single.error(failure(LegacyError.Network)) }
        val controller = launchScreen(presenters) { VoucherFragment() }
        (ShadowDialog.getLatestDialog() as AlertDialog)
            .getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        idle()

        val activity = controller.recreated()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_constancia_mensaje).visibility)
        assertEquals(View.GONE, activity.web().visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_constancia_finalizar).visibility)
        assertEquals(1, presenters.payments.vouchers.size)
    }

    private fun prepareForPin() {
        presenters.flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        presenters.flow.chooseAccount(Samples.gastos)
    }

    private fun LegacyHostActivity.debtRows(): List<View> {
        val list = findViewById<RecyclerView>(R.id.legacy_recibos_lista)
        return (0 until list.childCount).map(list::getChildAt)
    }

    private fun LegacyHostActivity.radios(): List<RadioButton> {
        val group = findViewById<RadioGroup>(R.id.legacy_cuenta_grupo)
        return (0 until group.childCount).map { group.getChildAt(it) as RadioButton }
    }

    private fun LegacyHostActivity.pad(): PinPadView = window.decorView.findViewWithTag("legacy_pin_pad")

    private fun LegacyHostActivity.dots() = findViewById<PinDotsView>(R.id.legacy_clave_puntos)

    private fun LegacyHostActivity.message() = findViewById<TextView>(R.id.legacy_clave_mensaje)

    private fun LegacyHostActivity.web(): WebView =
        findViewById<ViewGroup>(R.id.legacy_constancia_contenedor).getChildAt(0) as WebView

    private fun LegacyHostActivity.press(vararg digits: Int) {
        val pad = pad()
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
}
