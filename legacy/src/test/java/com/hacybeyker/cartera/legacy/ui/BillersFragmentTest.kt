package com.hacybeyker.cartera.legacy.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.appbar.MaterialToolbar
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.failure
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
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
class BillersFragmentTest {
    private val presenters = TestPresenters().apply { billers.respond = { Single.just(Samples.billers) } }

    private fun launch() = launchScreen(presenters) { BillersFragment() }

    private fun rows(root: View): List<View> {
        val list = root.findViewById<RecyclerView>(R.id.legacy_empresas_lista)
        return (0 until list.childCount).map(list::getChildAt)
    }

    private fun texts(root: View) = rows(root).map { (it as TextView).text.toString() }

    private fun allViews(view: View): List<View> {
        val children = (view as? ViewGroup)?.let { group -> (0 until group.childCount).map(group::getChildAt) }
        return listOf(view) + children.orEmpty().flatMap(::allViews)
    }

    @Test
    fun `the list shows the categories and the billers as plain text rows`() {
        val activity = launch().get()

        assertEquals(
            listOf(
                "Electricidad",
                "Luz del Norte",
                "Electro Sierra",
                "Agua",
                "Agua Trujillo",
                "Telefonía",
                "Telefonía Andina"
            ),
            texts(activity.window.decorView)
        )
    }

    @Test
    fun `the bar shows the title of the screen`() {
        val activity = launch().get()

        assertEquals("Empresas", activity.findViewById<MaterialToolbar>(R.id.legacy_actividad_barra).title)
    }

    @Test
    fun `the rows have no id and no content description of their own`() {
        val activity = launch().get()

        val rowViews = rows(activity.window.decorView).flatMap(::allViews)

        assertTrue(rowViews.isNotEmpty())
        rowViews.forEach { view ->
            assertEquals(View.NO_ID, view.id)
            assertEquals(null, view.contentDescription)
        }
    }

    @Test
    fun `the category rows are headings for accessibility and the biller rows are clickable`() {
        val activity = launch().get()

        val rows = rows(activity.window.decorView)

        assertTrue(ViewCompat.isAccessibilityHeading(rows[0]))
        assertFalse(rows[0].isClickable)
        assertTrue(rows[1].isClickable)
    }

    @Test
    fun `tapping a biller keeps it and opens the customer code`() {
        val activity = launch().get()

        rows(activity.window.decorView)[4].performClick()

        assertEquals(Samples.agua, presenters.flow.biller)
        assertEquals(listOf(LegacyScreen.CUSTOMER_CODE), HostEnvironment.navigated)
    }

    @Test
    fun `while loading the progress shows and with no results the empty text shows`() {
        presenters.billers.respond = { Single.never() }
        val loading = launch().get()
        assertEquals(View.VISIBLE, loading.findViewById<View>(R.id.legacy_empresas_cargando).visibility)
        assertEquals(View.GONE, loading.findViewById<View>(R.id.legacy_empresas_lista).visibility)

        presenters.billers.respond = { Single.just(emptyList()) }
        val empty = launch().get()
        assertEquals(View.VISIBLE, empty.findViewById<View>(R.id.legacy_empresas_vacio).visibility)
        assertEquals(
            "No se encontraron empresas",
            empty.findViewById<TextView>(R.id.legacy_empresas_vacio).text.toString()
        )
    }

    @Test
    fun `typing in the search box searches after the pause`() {
        val activity = launch().get()
        presenters.billers.respond = { Single.just(listOf(Samples.agua)) }

        activity.findViewById<TextView>(R.id.legacy_empresas_buscar).text = "agua"
        presenters.schedulers.clock.advanceTimeBy(300, TimeUnit.MILLISECONDS)
        idle()
        layoutNow(activity.window.decorView)

        assertEquals(listOf("", "agua"), presenters.billers.requests)
    }

    @Test
    fun `a network error shows an appcompat dialog with retry and cancel`() {
        presenters.billers.respond = { Single.error(failure(LegacyError.Network)) }
        launch()

        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertTrue(dialog.isShowing)
        assertEquals("Reintentar", dialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())
        assertEquals("Cancelar", dialog.getButton(AlertDialog.BUTTON_NEGATIVE).text.toString())

        presenters.billers.respond = { Single.just(Samples.billers) }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()

        assertFalse(dialog.isShowing)
        assertEquals(listOf("", ""), presenters.billers.requests)
    }

    @Test
    fun `cancelling the error dialog goes back`() {
        presenters.billers.respond = { Single.error(failure(LegacyError.Server)) }
        launch()

        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        idle()

        assertEquals(1, HostEnvironment.backs)
    }

    @Test
    fun `an expired session closes the module`() {
        presenters.billers.respond = { Single.error(failure(LegacyError.SessionExpired)) }

        launch()

        assertEquals(1, HostEnvironment.closes)
    }
}
