package com.hacybeyker.cartera.legacy.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.customer.CodeProblem
import com.hacybeyker.cartera.legacy.customer.CustomerCodeView
import com.hacybeyker.cartera.legacy.customer.CustomerCodeViewState
import com.hacybeyker.cartera.legacy.databinding.LegacyFragmentCustomerCodeBinding

internal class CustomerCodeFragment :
    LegacyScreenFragment(R.layout.legacy_fragment_customer_code, R.string.legacy_customer_title),
    CustomerCodeView {

    private val presenter by presenterOf { it.customerCode() }
    private var binding: LegacyFragmentCustomerCodeBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val views = LegacyFragmentCustomerCodeBinding.bind(view)
        binding = views
        views.legacyCodigoCampo.doAfterTextChanged { presenter.onCodeChanged(it?.toString().orEmpty()) }
        views.legacyCodigoContinuar.setOnClickListener { presenter.onContinue() }
        presenter.attach(this)
    }

    override fun onDestroyView() {
        presenter.detach()
        binding = null
        super.onDestroyView()
    }

    override fun render(state: CustomerCodeViewState) {
        val views = binding ?: return
        views.legacyCodigoEmpresa.text = state.billerName
        views.legacyCodigoIndicacion.text = getString(R.string.legacy_customer_prompt, state.billerName)
        val field = views.legacyCodigoCampo
        if (field.text.toString() != state.code) {
            field.setText(state.code)
            field.setSelection(state.code.length)
        }
        views.legacyCodigoContenedor.error = state.problem?.let { getString(it.messageRes()) }
    }

    override fun showDebts() = legacyHost.navigator.navigateTo(LegacyScreen.DEBTS)

    @StringRes
    private fun CodeProblem.messageRes(): Int = when (this) {
        CodeProblem.EMPTY -> R.string.legacy_customer_error_empty
        CodeProblem.INVALID_FORMAT -> R.string.legacy_customer_error_format
        CodeProblem.UNKNOWN_CUSTOMER -> R.string.legacy_customer_error_unknown
    }
}
