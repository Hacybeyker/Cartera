package com.hacybeyker.cartera.feature.legacy.ui

import androidx.lifecycle.ViewModel
import com.hacybeyker.cartera.feature.legacy.domain.usecase.RegisterBillPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LegacyViewModel @Inject constructor(private val registerBillPayment: RegisterBillPaymentUseCase) : ViewModel() {

    /** `:legacy` devolvió `Paid`: sube la revisión de las cuentas antes de que la pantalla anterior vuelva a mostrarse. */
    fun onPaid() = registerBillPayment()
}
