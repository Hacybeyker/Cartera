package com.hacybeyker.cartera.legacy.payment.domain

import io.reactivex.rxjava3.core.Single

interface PaymentRepository {
    fun pay(order: PaymentOrder): Single<BillPayment>

    /** La constancia del pago [paymentId] como HTML. */
    fun voucher(paymentId: String): Single<String>
}
