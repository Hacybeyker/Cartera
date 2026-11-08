package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.core.view.isVisible
import com.google.android.material.radiobutton.MaterialRadioButton
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.account.AccountOption
import com.hacybeyker.cartera.legacy.account.AccountView
import com.hacybeyker.cartera.legacy.account.AccountViewState
import com.hacybeyker.cartera.legacy.account.SummaryLine
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.databinding.LegacyFragmentAccountBinding
import com.hacybeyker.cartera.legacy.databinding.LegacyRowSummaryBinding

internal class AccountFragment :
    LegacyScreenFragment(R.layout.legacy_fragment_account, R.string.legacy_account_title),
    AccountView {

    private val presenter by presenterOf { it.account() }
    private var binding: LegacyFragmentAccountBinding? = null
    private var errorDialog: ErrorDialog? = null
    private var options: List<AccountOption> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val views = LegacyFragmentAccountBinding.bind(view)
        binding = views
        options = emptyList()
        errorDialog = ErrorDialog(requireContext(), presenter::onRetry, presenter::onCancel)
        views.legacyCuentaContinuar.setOnClickListener { presenter.onContinue() }
        views.legacyCuentaGrupo.setOnCheckedChangeListener { group, checkedId ->
            group.findViewById<View>(checkedId)?.let { presenter.onAccountSelected(it.tag as String) }
        }
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        errorDialog?.dismiss()
        errorDialog = null
        binding = null
        super.onDestroyView()
    }

    override fun render(state: AccountViewState) {
        val views = binding ?: return
        views.legacyCuentaEmpresa.text = state.billerName
        renderSummary(views, state.summary)
        views.legacyCuentaTotal.text = state.totalLabel
        views.legacyCuentaCargando.isVisible = state.phase == LoadPhase.LOADING
        views.legacyCuentaVacio.isVisible = state.phase == LoadPhase.EMPTY
        views.legacyCuentaGrupo.isVisible = state.phase == LoadPhase.CONTENT
        renderOptions(views.legacyCuentaGrupo, state)
        views.legacyCuentaAviso.isVisible = state.insufficientFunds
        views.legacyCuentaContinuar.isEnabled = state.canContinue
        errorDialog?.show(state.error)
    }

    override fun showPin() = legacyHost.navigator.navigateTo(LegacyScreen.PIN)

    override fun goBack() = legacyHost.navigator.goBack()

    override fun close() = legacyHost.navigator.close()

    private fun renderSummary(views: LegacyFragmentAccountBinding, lines: List<SummaryLine>) {
        val container = views.legacyCuentaResumen
        if (container.childCount == lines.size) return
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        lines.forEach { line ->
            val row = LegacyRowSummaryBinding.inflate(inflater, container, false)
            row.legacyCuentaResumenPeriodo.text = line.periodLabel
            row.legacyCuentaResumenImporte.text = line.amountLabel
            container.addView(row.root)
        }
    }

    private fun renderOptions(group: RadioGroup, state: AccountViewState) {
        if (options != state.options) {
            options = state.options
            group.removeAllViews()
            state.options.forEach { group.addView(radioFor(group, it)) }
        }
        val selected = (0 until group.childCount).map(group::getChildAt).firstOrNull { it.tag == state.selectedId }
        if (selected == null) group.clearCheck() else group.check(selected.id)
    }

    private fun radioFor(group: ViewGroup, option: AccountOption) = MaterialRadioButton(group.context).apply {
        id = View.generateViewId()
        tag = option.id
        isSaveEnabled = false
        minHeight = resources.getDimensionPixelSize(R.dimen.legacy_row_min_height)
        text = getString(R.string.legacy_account_option, option.alias, option.numberMasked, option.balanceLabel)
    }
}
