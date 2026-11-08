package com.hacybeyker.cartera.legacy.ui

import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.appbar.MaterialToolbar
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.failure
import io.reactivex.rxjava3.core.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class AccountFragmentTest {
    private val presenters = TestPresenters().apply {
        flow.chooseBiller(Samples.luz)
        flow.chooseCustomerCode("100200")
        flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        accounts.respond = { Single.just(listOf(Samples.sueldo, Samples.gastos, Samples.dolares)) }
    }

    private fun launch() = launchScreen(presenters) { AccountFragment() }

    private fun LegacyHostActivity.radios(): List<RadioButton> {
        val group = findViewById<RadioGroup>(R.id.legacy_cuenta_grupo)
        return (0 until group.childCount).map { group.getChildAt(it) as RadioButton }
    }

    @Test
    fun `it shows the summary of the debts, the total and only the accounts in soles, none chosen`() {
        val activity = launch().get()

        assertEquals("Cuenta y resumen", activity.findViewById<MaterialToolbar>(R.id.legacy_actividad_barra).title)
        assertEquals("S/ 177.50", activity.findViewById<TextView>(R.id.legacy_cuenta_total).text.toString())
        val summary = activity.findViewById<android.view.ViewGroup>(R.id.legacy_cuenta_resumen)
        assertEquals(2, summary.childCount)
        assertEquals(
            "Octubre 2026",
            summary.getChildAt(0).findViewById<TextView>(R.id.legacy_cuenta_resumen_periodo).text.toString()
        )
        assertEquals(
            "S/ 92.10",
            summary.getChildAt(1).findViewById<TextView>(R.id.legacy_cuenta_resumen_importe).text.toString()
        )
        assertEquals(
            listOf(
                "Cuenta Sueldo\n****4821 · Disponible S/ 8,450.75",
                "Cuenta Gastos\n****9034 · Disponible S/ 2,300.00"
            ),
            activity.radios().map { it.text.toString() }
        )
        assertTrue(activity.radios().none { it.isChecked })
        assertFalse(activity.findViewById<Button>(R.id.legacy_cuenta_continuar).isEnabled)
    }

    @Test
    fun `choosing an account enables continue and continuing opens the pin`() {
        val activity = launch().get()

        activity.radios()[1].performClick()
        idle()
        assertTrue(activity.radios()[1].isChecked)
        assertTrue(activity.findViewById<Button>(R.id.legacy_cuenta_continuar).isEnabled)
        activity.findViewById<Button>(R.id.legacy_cuenta_continuar).performClick()

        assertEquals(listOf(LegacyScreen.PIN), HostEnvironment.navigated)
        assertEquals(Samples.gastos, presenters.flow.account)
    }

    @Test
    fun `a balance below the total shows the warning without blocking continue`() {
        presenters.accounts.respond = { Single.just(listOf(Samples.poca)) }
        val activity = launch().get()
        assertEquals(View.GONE, activity.findViewById<View>(R.id.legacy_cuenta_aviso).visibility)

        activity.radios()[0].performClick()
        idle()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_cuenta_aviso).visibility)
        assertTrue(activity.findViewById<Button>(R.id.legacy_cuenta_continuar).isEnabled)
    }

    @Test
    fun `without accounts in soles the empty message shows`() {
        presenters.accounts.respond = { Single.just(listOf(Samples.dolares)) }

        val activity = launch().get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_cuenta_vacio).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.legacy_cuenta_grupo).visibility)
    }

    @Test
    fun `while loading only the progress shows in place of the accounts`() {
        presenters.accounts.respond = { Single.never() }

        val activity = launch().get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_cuenta_cargando).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.legacy_cuenta_grupo).visibility)
    }

    @Test
    fun `a network error shows the dialog and retry loads again`() {
        presenters.accounts.respond = { Single.error(failure(LegacyError.Network)) }
        launch()
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog

        presenters.accounts.respond = { Single.just(listOf(Samples.gastos)) }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()

        assertEquals(2, presenters.accounts.requests)
    }

    @Test
    fun `an expired session closes the module`() {
        presenters.accounts.respond = { Single.error(failure(LegacyError.SessionExpired)) }

        launch()

        assertEquals(1, HostEnvironment.closes)
    }
}
