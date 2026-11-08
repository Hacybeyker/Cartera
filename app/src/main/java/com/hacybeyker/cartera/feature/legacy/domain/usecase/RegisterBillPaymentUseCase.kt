package com.hacybeyker.cartera.feature.legacy.domain.usecase

import com.hacybeyker.cartera.core.domain.account.AccountChanges
import javax.inject.Inject

/** Un pago de servicios movió dinero en `:legacy`: las cuentas y los movimientos se vuelven a pedir al mostrarse. */
class RegisterBillPaymentUseCase @Inject constructor(private val changes: AccountChanges) {
    operator fun invoke() = changes.notifyChanged()
}
