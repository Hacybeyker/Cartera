package com.hacybeyker.cartera.legacy.customer

import com.hacybeyker.cartera.legacy.core.mvp.MvpView

enum class CodeProblem { EMPTY, INVALID_FORMAT, UNKNOWN_CUSTOMER }

data class CustomerCodeViewState(val billerName: String, val code: String, val problem: CodeProblem? = null)

interface CustomerCodeView : MvpView<CustomerCodeViewState> {
    fun showDebts()
}
