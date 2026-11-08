package com.hacybeyker.cartera.legacy.payment.data.dto

import com.hacybeyker.cartera.legacy.core.data.dto.MoneyDto
import com.hacybeyker.cartera.legacy.core.data.dto.toMoney
import com.hacybeyker.cartera.legacy.payment.domain.BillPayment
import com.hacybeyker.cartera.legacy.payment.domain.PaymentOrder
import kotlinx.serialization.Serializable

@Serializable
data class BillPaymentRequestDto(
    val billerId: String,
    val customerCode: String,
    val debtIds: List<String>,
    val accountId: String,
    val pin: String
)

@Serializable
data class BillPaymentDto(
    val paymentId: String,
    val operationNumber: String,
    val total: MoneyDto,
    val accountAvailableBalance: MoneyDto
)

fun PaymentOrder.toRequest(): BillPaymentRequestDto =
    BillPaymentRequestDto(billerId, customerCode, debtIds, accountId, pin)

fun BillPaymentDto.toBillPayment(): BillPayment =
    BillPayment(paymentId, operationNumber, total.toMoney(), accountAvailableBalance.toMoney())
