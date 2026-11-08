package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.databinding.LegacyFragmentVoucherBinding
import com.hacybeyker.cartera.legacy.voucher.VoucherView
import com.hacybeyker.cartera.legacy.voucher.VoucherViewState

internal class VoucherFragment :
    LegacyScreenFragment(R.layout.legacy_fragment_voucher, R.string.legacy_voucher_title),
    VoucherView {

    private val presenter by presenterOf { it.voucher() }
    private var binding: LegacyFragmentVoucherBinding? = null
    private var errorDialog: ErrorDialog? = null
    private var web: WebView? = null
    private var loadedHtml: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val views = LegacyFragmentVoucherBinding.bind(view)
        binding = views
        errorDialog = ErrorDialog(requireContext(), presenter::onRetry, presenter::onCancel)
        web = createWebView().also { views.legacyConstanciaContenedor.addView(it, 0) }
        loadedHtml = null
        views.legacyConstanciaFinalizar.setOnClickListener { presenter.onFinish() }
        // Atrás en la constancia equivale a Finalizar: el pago ya está hecho y no hay pantalla a la que volver.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { presenter.onFinish() }
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        errorDialog?.dismiss()
        errorDialog = null
        web?.let { (it.parent as? ViewGroup)?.removeView(it) }
        web?.destroy()
        web = null
        binding = null
        super.onDestroyView()
    }

    override fun render(state: VoucherViewState) {
        val views = binding ?: return
        val content = state.phase == LoadPhase.CONTENT
        views.legacyConstanciaCargando.isVisible = state.phase == LoadPhase.LOADING
        views.legacyConstanciaMensaje.isVisible = state.phase == LoadPhase.FAILED && state.error == null
        web?.let { page ->
            page.isVisible = content
            state.html?.takeIf { content && it != loadedHtml }?.let { html ->
                loadedHtml = html
                page.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        }
        errorDialog?.show(state.error)
    }

    override fun close() = legacyHost.navigator.close()

    // Sin `android:id` a propósito (S2): la constancia se lee por su texto. Sin JavaScript, archivos, contenido ni red.
    private fun createWebView(): WebView = WebView(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        settings.apply {
            javaScriptEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkLoads = true
            setSupportZoom(false)
            builtInZoomControls = false
        }
        isVisible = false
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) = presenter.onPageFinished()
        }
    }
}
