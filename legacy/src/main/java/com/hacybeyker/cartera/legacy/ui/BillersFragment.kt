package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.billers.BillerRow
import com.hacybeyker.cartera.legacy.billers.BillersView
import com.hacybeyker.cartera.legacy.billers.BillersViewState
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.databinding.LegacyFragmentBillersBinding

internal class BillersFragment :
    LegacyScreenFragment(R.layout.legacy_fragment_billers, R.string.legacy_billers_title),
    BillersView {

    private val presenter by presenterOf { it.billers() }
    private val adapter = BillersAdapter { presenter.onBillerSelected(it) }
    private var binding: LegacyFragmentBillersBinding? = null
    private var errorDialog: ErrorDialog? = null
    private var shownResultsFor: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val views = LegacyFragmentBillersBinding.bind(view)
        binding = views
        errorDialog = ErrorDialog(requireContext(), presenter::onRetry, presenter::onCancel)
        shownResultsFor = null
        views.legacyEmpresasLista.adapter = adapter
        views.legacyEmpresasBuscar.doAfterTextChanged { presenter.onQueryChanged(it?.toString().orEmpty()) }
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        errorDialog?.dismiss()
        errorDialog = null
        binding = null
        super.onDestroyView()
    }

    override fun render(state: BillersViewState) {
        val views = binding ?: return
        submitRows(views.legacyEmpresasLista, state)
        views.legacyEmpresasCargando.isVisible = state.phase == LoadPhase.LOADING
        views.legacyEmpresasLista.isVisible = state.rows.isNotEmpty()
        views.legacyEmpresasVacio.isVisible = state.phase == LoadPhase.EMPTY
        val search = views.legacyEmpresasBuscar
        if (search.text.toString() != state.query) {
            search.setText(state.query)
            search.setSelection(state.query.length)
        }
        errorDialog?.show(state.error)
    }

    private fun submitRows(list: RecyclerView, state: BillersViewState) {
        val resultsFor = state.resultsFor
        val newQuery = resultsFor != null && shownResultsFor != null && resultsFor != shownResultsFor
        shownResultsFor = resultsFor ?: shownResultsFor
        if (state.fullRebind) {
            rebindRows(list, state.rows, newQuery)
            return
        }
        if (list.adapter !== adapter) list.adapter = adapter
        if (newQuery) {
            adapter.submitList(state.rows) { list.scrollToPosition(0) }
        } else {
            adapter.submitList(state.rows)
        }
    }

    private fun rebindRows(list: RecyclerView, rows: List<BillerRow>, newQuery: Boolean) {
        val layout = list.layoutManager
        val position = layout?.onSaveInstanceState()
        val rebuilt = FullBillersAdapter(rows) { presenter.onBillerSelected(it) }
        list.adapter = rebuilt
        rebuilt.notifyDataSetChanged()
        if (newQuery) list.scrollToPosition(0) else layout?.onRestoreInstanceState(position)
    }

    override fun showCustomerCode() = legacyHost.navigator.navigateTo(LegacyScreen.CUSTOMER_CODE)

    override fun goBack() = legacyHost.navigator.goBack()

    override fun close() = legacyHost.navigator.close()
}
