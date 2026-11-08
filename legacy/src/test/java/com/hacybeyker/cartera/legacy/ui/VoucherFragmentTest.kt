package com.hacybeyker.cartera.legacy.ui

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class VoucherFragmentTest {
    private val presenters = TestPresenters().apply {
        flow.chooseBiller(Samples.luz)
        flow.completePayment(Samples.payment)
        payments.respondVoucher = { Single.just(Samples.voucherHtml) }
    }

    private fun launch() = launchScreen(presenters) { VoucherFragment() }

    private fun LegacyHostActivity.container() = findViewById<ViewGroup>(R.id.legacy_constancia_contenedor)

    private fun LegacyHostActivity.web(): WebView = container().getChildAt(0) as WebView

    @Test
    fun `it shows the title and the finish button, and the webview has no id`() {
        val activity = launch().get()

        assertEquals("Constancia", activity.findViewById<MaterialToolbar>(R.id.legacy_actividad_barra).title)
        assertEquals("Finalizar", activity.findViewById<Button>(R.id.legacy_constancia_finalizar).text.toString())
        assertEquals(View.NO_ID, activity.web().id)
        assertEquals(null, activity.web().contentDescription)
    }

    @Test
    fun `the webview is locked down, without javascript, files, content, network or zoom`() {
        val activity = launch().get()

        val settings = activity.web().settings

        assertFalse(settings.javaScriptEnabled)
        assertFalse(settings.allowFileAccess)
        assertFalse(settings.allowContentAccess)
        assertTrue(settings.blockNetworkLoads)
        assertFalse(settings.builtInZoomControls)
        assertFalse(settings.supportZoom())
    }

    @Test
    fun `the html of the backend goes to the webview as it came`() {
        val activity = launch().get()

        val loaded = shadowOf(activity.web()).lastLoadDataWithBaseURL

        assertNotNull(loaded)
        assertEquals(Samples.voucherHtml, loaded.data)
        assertEquals("text/html", loaded.mimeType)
        assertEquals("utf-8", loaded.encoding)
        assertEquals(null, loaded.baseUrl)
        assertEquals(View.VISIBLE, activity.web().visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.legacy_constancia_cargando).visibility)
        assertEquals(listOf("PAY-000001"), presenters.payments.vouchers)
    }

    @Test
    fun `the content mark comes after the page finished and only once`() {
        val activity = launch().get()
        assertEquals(emptyList<String>(), presenters.marker.marks)

        val web = activity.web()
        shadowOf(web).webViewClient.onPageFinished(web, "about:blank")
        shadowOf(web).webViewClient.onPageFinished(web, "about:blank")

        assertEquals(listOf("legacy_constancia"), presenters.marker.marks)
    }

    @Test
    fun `while loading only the progress shows`() {
        presenters.payments.respondVoucher = { Single.never() }

        val activity = launch().get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_constancia_cargando).visibility)
        assertEquals(View.GONE, activity.web().visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_constancia_finalizar).visibility)
        assertEquals(null, shadowOf(activity.web()).lastLoadDataWithBaseURL)
    }

    @Test
    fun `finish closes the module`() {
        val activity = launch().get()

        activity.findViewById<Button>(R.id.legacy_constancia_finalizar).performClick()

        assertEquals(1, HostEnvironment.closes)
    }

    @Test
    fun `back is the same as finish`() {
        val activity = launch().get()

        activity.onBackPressedDispatcher.onBackPressed()

        assertEquals(1, HostEnvironment.closes)
    }

    @Test
    fun `a network error shows the dialog and retrying loads the voucher`() {
        presenters.payments.respondVoucher = { Single.error(failure(LegacyError.Network)) }
        val activity = launch().get()
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertEquals("Reintentar", dialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())

        presenters.payments.respondVoucher = { Single.just(Samples.voucherHtml) }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()

        assertEquals(2, presenters.payments.vouchers.size)
        assertEquals(Samples.voucherHtml, shadowOf(activity.web()).lastLoadDataWithBaseURL.data)
    }

    @Test
    fun `cancelling the error leaves the message and the finish button, since the payment is already made`() {
        presenters.payments.respondVoucher = { Single.error(failure(LegacyError.Server)) }
        val activity = launch().get()

        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        idle()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.legacy_constancia_mensaje).visibility)
        assertEquals(
            "No pudimos cargar la constancia, pero tu pago se realizó.",
            activity.findViewById<TextView>(R.id.legacy_constancia_mensaje).text.toString()
        )
        assertEquals(0, HostEnvironment.backs)
        activity.findViewById<Button>(R.id.legacy_constancia_finalizar).performClick()
        assertEquals(1, HostEnvironment.closes)
    }

    @Test
    fun `an expired session closes the module`() {
        presenters.payments.respondVoucher = { Single.error(failure(LegacyError.SessionExpired)) }

        launch()

        assertEquals(1, HostEnvironment.closes)
    }
}
