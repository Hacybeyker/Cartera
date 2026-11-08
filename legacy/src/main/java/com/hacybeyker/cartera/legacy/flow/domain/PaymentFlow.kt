package com.hacybeyker.cartera.legacy.flow.domain

import com.hacybeyker.cartera.legacy.account.domain.Account
import com.hacybeyker.cartera.legacy.billers.domain.Biller
import com.hacybeyker.cartera.legacy.core.domain.Money
import com.hacybeyker.cartera.legacy.debts.domain.Debt
import com.hacybeyker.cartera.legacy.debts.domain.total
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

/**
 * Lo que la persona fue eligiendo en el pago de servicios: vive lo que dura la Activity, también al rotar. Cada
 * elección descarta las posteriores, que dependían de ella. Solo se toca desde el hilo principal.
 */
@ActivityRetainedScoped
class PaymentFlow @Inject constructor() {

    /** Si la Activity se recrea con otro proceso, el flujo vuelve sin empezar y la Activity se cierra. */
    var isStarted: Boolean = false
        private set

    var biller: Biller? = null
        private set

    var customerCode: String? = null
        private set

    var customerName: String? = null
        private set

    var debts: List<Debt> = emptyList()
        private set

    var account: Account? = null
        private set

    private var rejectedCustomerCode: String? = null

    val total: Money
        get() = debts.total()

    fun start() {
        isStarted = true
    }

    fun chooseBiller(biller: Biller) {
        this.biller = biller
        customerCode = null
        rejectedCustomerCode = null
        chooseDebts(customerName = null, debts = emptyList())
    }

    fun chooseCustomerCode(code: String) {
        customerCode = code
        rejectedCustomerCode = null
        chooseDebts(customerName = null, debts = emptyList())
    }

    fun chooseDebts(customerName: String?, debts: List<Debt>) {
        this.customerName = customerName
        this.debts = debts
        account = null
    }

    fun chooseAccount(account: Account) {
        this.account = account
    }

    /** El servidor no conoce el código elegido: la pantalla del código lo avisa al volver a mostrarse. */
    fun rejectCustomerCode() {
        rejectedCustomerCode = customerCode
        chooseDebts(customerName = null, debts = emptyList())
    }

    /** Devuelve el código que el servidor rechazó, una sola vez. */
    fun takeRejectedCustomerCode(): String? = rejectedCustomerCode.also { rejectedCustomerCode = null }
}
