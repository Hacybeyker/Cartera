package com.hacybeyker.cartera.legacy.payment.domain

import com.hacybeyker.cartera.legacy.core.domain.Money

data class BillPayment(val paymentId: String, val operationNumber: String, val total: Money, val newBalance: Money)
