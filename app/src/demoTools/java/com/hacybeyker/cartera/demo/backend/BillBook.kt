package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.time.Instant

/** Un recibo pagado, tal como lo mostraron la respuesta y la constancia. */
data class PaidDebt(val id: String, val period: String, val amount: BigDecimal)

/**
 * Un pago ya hecho. Guarda lo que la constancia necesita (empresa, cuenta y saldo de ese momento): así no cambia
 * aunque la cuenta se mueva después.
 */
data class BillPayment(
    val id: String,
    val userId: String,
    val operationNumber: String,
    val createdAt: Instant,
    val billerName: String,
    val customerCode: String,
    val customerName: String,
    val paidDebts: List<PaidDebt>,
    val currency: String,
    val accountId: String,
    val accountLabel: String,
    val newBalance: BigDecimal
) {
    val total: BigDecimal get() = paidDebts.fold(BigDecimal.ZERO) { sum, debt -> sum + debt.amount }
}

/**
 * Empresas, clientes y recibos de los fixtures, más los recibos pagados y los pagos hechos. Lo pagado vive en
 * memoria: vuelve a los fixtures con cada proceso y con el reset.
 */
class BillBook(private val catalog: FixtureCatalog) {
    private val paidDebtIds = mutableSetOf<String>()
    private val payments = mutableMapOf<String, BillPayment>()

    val billers: List<Biller> get() = catalog.billers

    fun biller(billerId: String): Biller? = catalog.billers.firstOrNull { it.id == billerId }

    fun customer(billerId: String, customerCode: String): BillerCustomer? =
        catalog.customers(billerId).firstOrNull { it.code == customerCode }

    /** Los recibos del cliente que aún no se pagaron. */
    fun pendingDebts(customer: BillerCustomer): List<Debt> = customer.debts.filterNot { isPaid(it.id) }

    fun isPaid(debtId: String): Boolean = debtId in paidDebtIds

    fun record(payment: BillPayment) {
        payments[payment.id] = payment
        paidDebtIds += payment.paidDebts.map { it.id }
    }

    /** Un pago de otra persona no existe para quien pregunta. */
    fun payment(userId: String, paymentId: String): BillPayment? = payments[paymentId]?.takeIf { it.userId == userId }

    fun reset() {
        paidDebtIds.clear()
        payments.clear()
    }
}
