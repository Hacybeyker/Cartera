package com.hacybeyker.cartera.legacy.debts.domain

import io.reactivex.rxjava3.core.Single

interface DebtRepository {
    /** Los recibos sin pagar del cliente [customerCode] de la empresa [billerId], del más antiguo al más reciente. */
    fun pendingDebts(billerId: String, customerCode: String): Single<CustomerDebts>
}
