package com.hacybeyker.cartera.legacy.customer

import com.hacybeyker.cartera.legacy.core.mvp.BasePresenter
import com.hacybeyker.cartera.legacy.customer.domain.CodeCheck
import com.hacybeyker.cartera.legacy.customer.domain.ValidateCustomerCodeUseCase
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import javax.inject.Inject

class CustomerCodePresenter @Inject constructor(
    private val flow: PaymentFlow,
    private val validate: ValidateCustomerCodeUseCase
) : BasePresenter<CustomerCodeView, CustomerCodeViewState>(
    CustomerCodeViewState(
        billerName = checkNotNull(flow.biller).name,
        code = flow.customerCode.orEmpty()
    )
) {

    // Al volver de Recibos con un código que el servidor no conoce, la pantalla lo dice bajo el campo.
    override fun onAttached() {
        val rejected = flow.takeRejectedCustomerCode() ?: return
        update { it.copy(code = rejected, problem = CodeProblem.UNKNOWN_CUSTOMER) }
    }

    fun onCodeChanged(text: String) {
        if (text == state.code) return
        update { it.copy(code = text, problem = null) }
    }

    fun onContinue() {
        val code = state.code.trim()
        val problem = when (validate(code, checkNotNull(flow.biller).customerCodeFormat)) {
            CodeCheck.VALID -> null
            CodeCheck.EMPTY -> CodeProblem.EMPTY
            CodeCheck.INVALID_FORMAT -> CodeProblem.INVALID_FORMAT
        }
        if (problem == null) {
            flow.chooseCustomerCode(code)
            emit { it.showDebts() }
        } else {
            update { it.copy(problem = problem) }
        }
    }
}
