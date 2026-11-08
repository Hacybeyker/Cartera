package com.hacybeyker.cartera.legacy.ui

import android.widget.Button
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.testing.Samples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class CustomerCodeFragmentTest {
    private val presenters = TestPresenters().apply { flow.chooseBiller(Samples.luz) }

    private fun launch() = launchScreen(presenters) { CustomerCodeFragment() }

    @Test
    fun `it shows the biller, the title and an empty field without error`() {
        val activity = launch().get()

        assertEquals("Luz del Norte", activity.findText(R.id.legacy_codigo_empresa))
        assertEquals("Código de cliente", activity.findViewById<MaterialToolbar>(R.id.legacy_actividad_barra).title)
        assertEquals("", activity.findViewById<TextInputEditText>(R.id.legacy_codigo_campo).text.toString())
        assertNull(activity.findViewById<TextInputLayout>(R.id.legacy_codigo_contenedor).error)
    }

    @Test
    fun `a code with the wrong format shows the message under the field and keeps the text`() {
        val activity = launch().get()
        val field = activity.findViewById<TextInputEditText>(R.id.legacy_codigo_campo)

        field.setText("1002")
        activity.findViewById<Button>(R.id.legacy_codigo_continuar).performClick()

        assertEquals(
            "El código no tiene el formato de esta empresa",
            activity.findViewById<TextInputLayout>(R.id.legacy_codigo_contenedor).error.toString()
        )
        assertEquals("1002", field.text.toString())
        assertEquals(emptyList<LegacyScreen>(), HostEnvironment.navigated)
    }

    @Test
    fun `a valid code opens the debts`() {
        val activity = launch().get()

        activity.findViewById<TextInputEditText>(R.id.legacy_codigo_campo).setText("100200")
        activity.findViewById<Button>(R.id.legacy_codigo_continuar).performClick()

        assertEquals(listOf(LegacyScreen.DEBTS), HostEnvironment.navigated)
        assertEquals("100200", presenters.flow.customerCode)
    }

    @Test
    fun `returning with a rejected code shows the message and the typed code`() {
        presenters.flow.chooseCustomerCode("999999")
        presenters.flow.rejectCustomerCode()

        val activity = launch().get()

        assertEquals("999999", activity.findViewById<TextInputEditText>(R.id.legacy_codigo_campo).text.toString())
        assertEquals(
            "No encontramos ese código de cliente en esta empresa",
            activity.findViewById<TextInputLayout>(R.id.legacy_codigo_contenedor).error.toString()
        )
    }

    private fun LegacyHostActivity.findText(id: Int): String = findViewById<android.widget.TextView>(id).text.toString()
}
