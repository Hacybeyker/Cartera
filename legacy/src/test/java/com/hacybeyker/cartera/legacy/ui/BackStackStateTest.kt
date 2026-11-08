package com.hacybeyker.cartera.legacy.ui

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.testing.Samples
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class BackStackStateTest {
    private val presenters = TestPresenters().apply {
        flow.chooseBiller(Samples.luz)
        billers.respond = { Single.just(Samples.billers) }
        debts.respond = { Single.just(Samples.luzDebts) }
        accounts.respond = { Single.just(listOf(Samples.sueldo, Samples.gastos)) }
    }

    @Test
    fun `the customer code keeps the rejected code message after going to the debts and back`() {
        val activity = launchScreen(presenters) { CustomerCodeFragment() }.get()
        activity.findViewById<TextInputEditText>(R.id.legacy_codigo_campo).setText("999999")
        activity.findViewById<Button>(R.id.legacy_codigo_continuar).performClick()

        activity.pushAndPop { presenters.flow.rejectCustomerCode() }

        assertEquals("999999", activity.findViewById<TextInputEditText>(R.id.legacy_codigo_campo).text.toString())
        assertEquals(
            "No encontramos ese código de cliente en esta empresa",
            activity.findViewById<TextInputLayout>(R.id.legacy_codigo_contenedor).error.toString()
        )
    }

    @Test
    fun `the customer code keeps the format message after going away and back`() {
        val activity = launchScreen(presenters) { CustomerCodeFragment() }.get()
        activity.findViewById<TextInputEditText>(R.id.legacy_codigo_campo).setText("1002")
        activity.findViewById<Button>(R.id.legacy_codigo_continuar).performClick()

        activity.pushAndPop()

        assertEquals("1002", activity.findViewById<TextInputEditText>(R.id.legacy_codigo_campo).text.toString())
        assertEquals(
            "El código no tiene el formato de esta empresa",
            activity.findViewById<TextInputLayout>(R.id.legacy_codigo_contenedor).error.toString()
        )
    }

    @Test
    fun `the billers keep the search text and the list after going to the customer code and back`() {
        val activity = launchScreen(presenters) { BillersFragment() }.get()
        presenters.billers.respond = { Single.just(listOf(Samples.agua)) }
        activity.findViewById<TextView>(R.id.legacy_empresas_buscar).text = "agua"
        presenters.schedulers.clock.advanceTimeBy(300, TimeUnit.MILLISECONDS)
        idle()

        activity.pushAndPop()

        assertEquals("agua", activity.findViewById<TextView>(R.id.legacy_empresas_buscar).text.toString())
        assertEquals(1, presenters.billers.requests.count { it == "agua" })
    }

    @Test
    fun `the account keeps the chosen radio after going to the pin and back`() {
        presenters.flow.chooseCustomerCode("100200")
        presenters.flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        val activity = launchScreen(presenters) { AccountFragment() }.get()
        val group = activity.findViewById<RadioGroup>(R.id.legacy_cuenta_grupo)
        group.getChildAt(1).performClick()
        idle()

        activity.pushAndPop()

        val radios = activity.findViewById<RadioGroup>(R.id.legacy_cuenta_grupo).let { current ->
            (0 until current.childCount).map { current.getChildAt(it) as RadioButton }
        }
        assertEquals(listOf(false, true), radios.map { it.isChecked })
        assertTrue(activity.findViewById<Button>(R.id.legacy_cuenta_continuar).isEnabled)
    }

    @Test
    fun `the debts keep the checked boxes and the total after going to the account and back`() {
        presenters.flow.chooseCustomerCode("100200")
        val activity = launchScreen(presenters) { DebtsFragment() }.get()
        activity.debtRows()[1].performClick()
        idle()

        activity.pushAndPop()

        assertEquals(
            listOf(false, true),
            activity.debtRows().map { it.findViewById<CheckBox>(R.id.legacy_recibos_casilla).isChecked }
        )
        assertEquals("S/ 92.10", activity.findViewById<TextView>(R.id.legacy_recibos_total).text.toString())
        assertTrue(activity.findViewById<Button>(R.id.legacy_recibos_continuar).isEnabled)
        assertFalse(activity.findViewById<View>(R.id.legacy_recibos_vacio).visibility == View.VISIBLE)
    }

    private fun LegacyHostActivity.debtRows(): List<View> {
        val list = findViewById<RecyclerView>(R.id.legacy_recibos_lista)
        return (0 until list.childCount).map(list::getChildAt)
    }
}
