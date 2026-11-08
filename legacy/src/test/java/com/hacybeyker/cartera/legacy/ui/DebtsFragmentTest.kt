package com.hacybeyker.cartera.legacy.ui

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
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
class DebtsFragmentTest {
    private val presenters = TestPresenters().apply {
        flow.chooseBiller(Samples.luz)
        flow.chooseCustomerCode("100200")
        debts.respond = { Single.just(Samples.luzDebts) }
    }

    private fun launch() = launchScreen(presenters) { DebtsFragment() }

    private fun LegacyHostActivity.rows(): List<View> {
        val list = findViewById<RecyclerView>(R.id.legacy_recibos_lista)
        return (0 until list.childCount).map(list::getChildAt)
    }

    @Test
    fun `it lists each debt with its period, due date and amount, unchecked, with a total of zero soles`() {
        val activity = launch().get()

        assertEquals("Ana Torres", activity.findViewById<TextView>(R.id.legacy_recibos_cliente).text.toString())
        assertEquals("Recibos pendientes", activity.findViewById<MaterialToolbar>(R.id.legacy_actividad_barra).title)
        val first = activity.rows()[0]
        assertEquals("Octubre 2026", first.findViewById<CheckBox>(R.id.legacy_recibos_casilla).text.toString())
        assertEquals(
            "Vence el 12/11/2026",
            first.findViewById<TextView>(R.id.legacy_recibos_vencimiento).text.toString()
        )
        assertEquals("S/ 85.40", first.findViewById<TextView>(R.id.legacy_recibos_importe).text.toString())
        assertFalse(first.findViewById<CheckBox>(R.id.legacy_recibos_casilla).isChecked)
        assertEquals("S/ 0.00", activity.findViewById<TextView>(R.id.legacy_recibos_total).text.toString())
        assertFalse(activity.findViewById<Button>(R.id.legacy_recibos_continuar).isEnabled)
    }

    @Test
    fun `tapping a row checks it, adds its amount and enables continue`() {
        val activity = launch().get()

        activity.rows()[0].performClick()
        idle()
        activity.rows()[1].performClick()
        idle()

        assertEquals("S/ 177.50", activity.findViewById<TextView>(R.id.legacy_recibos_total).text.toString())
        assertTrue(activity.findViewById<Button>(R.id.legacy_recibos_continuar).isEnabled)
    }

    @Test
    fun `continuing opens the account with the checked debts in the flow`() {
        val activity = launch().get()
        activity.rows()[1].performClick()
        idle()

        activity.findViewById<Button>(R.id.legacy_recibos_continuar).performClick()

        assertEquals(listOf(LegacyScreen.ACCOUNT), HostEnvironment.navigated)
        assertEquals(listOf(Samples.noviembre), presenters.flow.debts)
    }

    @Test
    fun `a customer without debts shows the empty message and no total`() {
        presenters.debts.respond = { Single.just(Samples.luzDebts.copy(debts = emptyList())) }

        val activity = launch().get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_recibos_vacio).visibility)
        assertEquals(
            "No tienes recibos pendientes",
            activity.findViewById<TextView>(R.id.legacy_recibos_vacio).text.toString()
        )
        assertEquals(View.GONE, activity.findViewById<View>(R.id.legacy_recibos_pie).visibility)
    }

    @Test
    fun `while loading only the progress shows`() {
        presenters.debts.respond = { Single.never() }

        val activity = launch().get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_recibos_cargando).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.legacy_recibos_lista).visibility)
    }

    @Test
    fun `an unknown customer goes back to the customer code`() {
        presenters.debts.respond = {
            Single.error(
                failure(
                    LegacyError.Rejected(
                        404,
                        com.hacybeyker.cartera.legacy.core.domain.ProblemCode.CUSTOMER_NOT_FOUND,
                        "no existe",
                        emptyList()
                    )
                )
            )
        }

        launch()

        assertEquals(1, HostEnvironment.backs)
        assertEquals("100200", presenters.flow.takeRejectedCustomerCode())
    }

    @Test
    fun `a server error shows the dialog and retry loads again`() {
        presenters.debts.respond = { Single.error(failure(LegacyError.Server)) }
        launch()
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertEquals("Reintentar", dialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())
        assertEquals("Cancelar", dialog.getButton(AlertDialog.BUTTON_NEGATIVE).text.toString())

        presenters.debts.respond = { Single.just(Samples.luzDebts) }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()

        assertEquals(2, presenters.debts.requests.size)
    }

    @Test
    fun `an expired session closes the module`() {
        presenters.debts.respond = { Single.error(failure(LegacyError.SessionExpired)) }

        launch()

        assertEquals(1, HostEnvironment.closes)
    }
}
