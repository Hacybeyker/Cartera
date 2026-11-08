package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.databinding.LegacyFragmentPinBinding
import com.hacybeyker.cartera.legacy.pin.PinView
import com.hacybeyker.cartera.legacy.pin.PinViewState
import com.hacybeyker.cartera.legacy.pin.domain.PinKey
import com.hacybeyker.cartera.legacy.ui.pinpad.PinPadView

private const val PAD_TAG = "legacy_pin_pad"

internal class PinFragment :
    LegacyScreenFragment(R.layout.legacy_fragment_pin, R.string.legacy_pin_title),
    PinView {

    private val presenter by presenterOf { it.pin() }
    private var binding: LegacyFragmentPinBinding? = null
    private var pad: PinPadView? = null
    private var errorDialog: ErrorDialog? = null
    private var noticeDialog: NoticeDialog? = null
    private var blockBack: OnBackPressedCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = LegacyFragmentPinBinding.bind(view)
        // El teclado no tiene `android:id` a propósito (S2): se lo encuentra por su etiqueta, que ninguna
        // herramienta de accesibilidad expone.
        pad = view.findViewWithTag<PinPadView>(PAD_TAG).also { it.onKeyPressed = ::onKey }
        errorDialog = ErrorDialog(requireContext(), presenter::onRetry, presenter::onCancel)
        noticeDialog = NoticeDialog(requireContext(), presenter::onNoticeAccepted)
        blockBack = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, enabled = false) {}
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        errorDialog?.dismiss()
        noticeDialog?.dismiss()
        errorDialog = null
        noticeDialog = null
        blockBack = null
        pad = null
        binding = null
        super.onDestroyView()
    }

    override fun render(state: PinViewState) {
        val views = binding ?: return
        views.legacyClavePuntos.filled = state.filled
        pad?.apply {
            order = state.order
            isEnabled = !state.sending
        }
        views.legacyClaveCargando.isVisible = state.sending
        views.legacyClaveMensaje.text = if (state.invalidPin) getString(R.string.legacy_pin_invalid) else ""
        views.legacyClaveMensaje.isInvisible = !state.invalidPin
        blockBack?.isEnabled = state.sending
        errorDialog?.show(state.error)
        noticeDialog?.show(state.notice)
    }

    override fun showVoucher() = legacyHost.navigator.replaceStackWith(LegacyScreen.VOUCHER)

    override fun goBack() = legacyHost.navigator.goBack()

    override fun reloadDebts() = legacyHost.navigator.restartAt(LegacyScreen.DEBTS)

    override fun close() = legacyHost.navigator.close()

    private fun onKey(key: PinKey) {
        when (key) {
            is PinKey.Digit -> presenter.onDigit(key.value)
            PinKey.Delete -> presenter.onDelete()
        }
    }
}
