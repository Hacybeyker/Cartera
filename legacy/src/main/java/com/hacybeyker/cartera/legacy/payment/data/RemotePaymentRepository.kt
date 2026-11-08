package com.hacybeyker.cartera.legacy.payment.data

import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.payment.data.dto.toBillPayment
import com.hacybeyker.cartera.legacy.payment.data.dto.toRequest
import com.hacybeyker.cartera.legacy.payment.domain.BillPayment
import com.hacybeyker.cartera.legacy.payment.domain.PaymentOrder
import com.hacybeyker.cartera.legacy.payment.domain.PaymentRepository
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class RemotePaymentRepository @Inject constructor(private val api: PaymentsApi, private val caller: LegacyApiCaller) :
    PaymentRepository {

    override fun pay(order: PaymentOrder): Single<BillPayment> = api.pay(order.idempotencyKey, order.toRequest())
        .map { it.toBillPayment() }
        .compose(caller.translated())

    override fun voucher(paymentId: String): Single<String> = api.voucher(paymentId)
        .map { it.string() }
        .compose(caller.translated())
}
