package com.hacybeyker.cartera.legacy.flow.domain

import com.hacybeyker.cartera.legacy.account.domain.Account
import com.hacybeyker.cartera.legacy.billers.domain.Biller
import com.hacybeyker.cartera.legacy.core.domain.Money
import com.hacybeyker.cartera.legacy.debts.domain.Debt
import com.hacybeyker.cartera.legacy.debts.domain.total
import com.hacybeyker.cartera.legacy.payment.domain.BillPayment
import com.hacybeyker.cartera.legacy.payment.domain.PaymentOrder
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

    /** El pago hecho; la Constancia lo lee y, mientras exista, el módulo se cierra como `Paid`. */
    var payment: BillPayment? = null
        private set

    private var mayHaveCharged = false
    private var rejectedCustomerCode: String? = null

    val total: Money
        get() = debts.total()

    /** Si el pago se hizo o pudo hacerse: quien aloja el módulo debe volver a pedir los saldos. */
    val accountsChanged: Boolean
        get() = payment != null || mayHaveCharged

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

    /** La orden de pago con lo elegido hasta aquí, la clave tecleada y la clave de idempotencia de este intento. */
    fun paymentOrder(pin: String, idempotencyKey: String): PaymentOrder = PaymentOrder(
        billerId = checkNotNull(biller).id,
        customerCode = checkNotNull(customerCode),
        debtIds = debts.map { it.id },
        accountId = checkNotNull(account).id,
        pin = pin,
        idempotencyKey = idempotencyKey
    )

    fun completePayment(payment: BillPayment) {
        this.payment = payment
    }

    /** El pago terminó sin respuesta o con un error del servidor: pudo cobrarse. */
    fun markMayHaveCharged() {
        mayHaveCharged = true
    }

    /** El servidor no conoce el código elegido: la pantalla del código lo avisa al volver a mostrarse. */
    fun rejectCustomerCode() {
        rejectedCustomerCode = customerCode
        chooseDebts(customerName = null, debts = emptyList())
    }

    /** Devuelve el código que el servidor rechazó, una sola vez. */
    fun takeRejectedCustomerCode(): String? = rejectedCustomerCode.also { rejectedCustomerCode = null }
}
