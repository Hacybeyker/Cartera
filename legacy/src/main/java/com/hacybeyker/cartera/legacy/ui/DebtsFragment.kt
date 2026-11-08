package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.databinding.LegacyFragmentDebtsBinding
import com.hacybeyker.cartera.legacy.debts.DebtsView
import com.hacybeyker.cartera.legacy.debts.DebtsViewState

internal class DebtsFragment :
    LegacyScreenFragment(R.layout.legacy_fragment_debts, R.string.legacy_debts_title),
    DebtsView {

    private val presenter by presenterOf { it.debts() }
    private val adapter = DebtsAdapter { presenter.onDebtToggled(it) }
    private var binding: LegacyFragmentDebtsBinding? = null
    private var errorDialog: ErrorDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val views = LegacyFragmentDebtsBinding.bind(view)
        binding = views
        errorDialog = ErrorDialog(requireContext(), presenter::onRetry, presenter::onCancel)
        views.legacyRecibosLista.adapter = adapter
        views.legacyRecibosContinuar.setOnClickListener { presenter.onContinue() }
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        errorDialog?.dismiss()
        errorDialog = null
        binding = null
        super.onDestroyView()
    }

    override fun render(state: DebtsViewState) {
        val views = binding ?: return
        val hasContent = state.phase == LoadPhase.CONTENT
        adapter.submitList(state.items)
        views.legacyRecibosCliente.text = state.customerName
        views.legacyRecibosCliente.isVisible = hasContent || state.phase == LoadPhase.EMPTY
        views.legacyRecibosCargando.isVisible = state.phase == LoadPhase.LOADING
        views.legacyRecibosLista.isVisible = hasContent
        views.legacyRecibosVacio.isVisible = state.phase == LoadPhase.EMPTY
        views.legacyRecibosPie.isVisible = hasContent
        views.legacyRecibosTotal.text = state.totalLabel
        views.legacyRecibosContinuar.isEnabled = state.canContinue
        errorDialog?.show(state.error)
    }

    override fun showAccount() = legacyHost.navigator.navigateTo(LegacyScreen.ACCOUNT)

    override fun returnToCustomerCode() = legacyHost.navigator.goBack()

    override fun goBack() = legacyHost.navigator.goBack()

    override fun close() = legacyHost.navigator.close()
}
