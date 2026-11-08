package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.time.Clock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Pago de servicios: empresas, recibos pendientes, el pago con clave de operaciones y su constancia. */
class BillDesk(private val state: BackendState, private val clock: Clock) {
    private val bills = state.bills
    private val parser = BillPaymentParser(bills::biller)

    fun billers(query: Map<String, String>): BackendResponse {
        val matches = BillerSearch.filter(bills.billers, query["query"].orEmpty())
        return json(OK, buildJsonObject { put("items", JsonArray(matches.map(Biller::toJson))) })
    }

    fun debts(billerId: String, query: Map<String, String>): BackendResponse {
        val biller = bills.biller(billerId)
            ?: throw ProblemException(ProblemCode.BILLER_NOT_FOUND, "No existe la empresa $billerId.")
        val request = DebtsQuery.parse(biller, query)
        val customer = bills.customer(biller.id, request.customerCode)
            ?: throw ProblemException(
                ProblemCode.CUSTOMER_NOT_FOUND,
                "No existe el código ${request.customerCode} en ${biller.name}."
            )
        return json(
            OK,
            buildJsonObject {
                put("customerName", customer.name)
                put("debts", JsonArray(bills.pendingDebts(customer).map(Debt::json)))
                if (request.includeHistory) put("history", customer.history)
            }
        )
    }

    fun pay(userId: String, idempotencyKey: String?, body: JsonObject): BackendResponse {
        if (idempotencyKey.isNullOrBlank()) {
            throw fieldProblem(TransferDesk.IDEMPOTENCY_HEADER, "REQUIRED", "La cabecera es obligatoria.")
        }
        return state.idempotency.once("$OPERATION:$userId:$idempotencyKey") { payOnce(userId, parser.parse(body)) }
    }

    fun voucher(userId: String, paymentId: String): BackendResponse {
        val payment = bills.payment(userId, paymentId)
            ?: throw ProblemException(ProblemCode.PAYMENT_NOT_FOUND, "No existe el pago $paymentId.")
        return BackendResponse(status = OK, body = BillVoucher.render(payment), contentType = HTML)
    }

    private fun payOnce(userId: String, request: BillPaymentRequest): BackendResponse {
        val account = ownAccount(userId, request.accountId)
        checkPin(userId, request.pin)
        val customer = bills.customer(request.biller.id, request.customerCode)
            ?: throw fieldProblem("customerCode", "UNKNOWN_CUSTOMER", "No existe ese código en la empresa.")
        val debts = payableDebts(customer, request.debtIds)
        val total = debts.fold(BigDecimal.ZERO) { sum, debt -> sum + debt.amount }
        val available = account.availableBalance()
        if (total > available) {
            throw ProblemException(ProblemCode.INSUFFICIENT_FUNDS, "El total supera el saldo disponible.")
        }
        return paid(Charge(request, customer, account, debts, available - total), userId)
    }

    private fun ownAccount(userId: String, accountId: String): JsonObject {
        val account = state.account(userId, accountId)
            ?: throw fieldProblem("accountId", "NOT_OWNED", "La cuenta no es tuya.")
        if (account.string("currency") != BillPaymentJson.CURRENCY) {
            throw ProblemException(ProblemCode.CURRENCY_MISMATCH, "Los recibos se pagan con una cuenta en soles.")
        }
        return account
    }

    private fun checkPin(userId: String, pin: String) {
        if (state.user(userId)?.text("operationsPin") != pin) {
            throw ProblemException(ProblemCode.INVALID_PIN, "La clave de operaciones es incorrecta.")
        }
    }

    /** Los recibos deben existir entre los del cliente; si alguno ya se pagó, no se cobra ninguno. */
    private fun payableDebts(customer: BillerCustomer, debtIds: List<String>): List<Debt> {
        val byId = customer.debts.associateBy { it.id }
        val unknown = debtIds.filter { it !in byId }
        if (unknown.isNotEmpty()) {
            throw fieldProblem("debtIds", "UNKNOWN_DEBT", "No son recibos de ese cliente: ${unknown.joinToString()}.")
        }
        val debts = debtIds.map { byId.getValue(it) }
        val paid = debts.filter { bills.isPaid(it.id) }
        if (paid.isNotEmpty()) {
            throw ProblemException(ProblemCode.DEBT_ALREADY_PAID, "Ya se pagó: ${paid.joinToString { it.id }}.")
        }
        return debts
    }

    private class Charge(
        val request: BillPaymentRequest,
        val customer: BillerCustomer,
        val account: JsonObject,
        val debts: List<Debt>,
        val newBalance: BigDecimal
    )

    private fun paid(charge: Charge, userId: String): BackendResponse {
        val sequence = state.nextSequence(OPERATION)
        val accountId = charge.account.string("id")
        val movementId = "MOV-${accountId.substringAfter("ACC-")}-P%04d".format(sequence)
        val payment = BillPayment(
            id = "PAY-%06d".format(sequence),
            userId = userId,
            operationNumber = "8%07d".format(sequence),
            createdAt = clock.instant(),
            billerName = charge.request.biller.name,
            customerCode = charge.request.customerCode,
            customerName = charge.customer.name,
            paidDebts = charge.debts.map { PaidDebt(it.id, it.period, it.amount) },
            currency = BillPaymentJson.CURRENCY,
            accountId = accountId,
            accountLabel = "${charge.account.string("alias")} ${charge.account.string("numberMasked")}",
            newBalance = charge.newBalance
        )
        state.post(accountId, charge.newBalance, MovementRecord.from(BillPaymentJson.movement(payment, movementId)))
        bills.record(payment)
        return json(CREATED, BillPaymentJson.receipt(payment, movementId))
    }

    private fun fieldProblem(field: String, code: String, message: String) = ProblemException(
        ProblemCode.VALIDATION_ERROR,
        "El pago tiene datos inválidos.",
        listOf(FieldError(field, code, message))
    )

    private companion object {
        const val HTML = "text/html"
        const val OPERATION = "bill_payments"
        const val OK = 200
        const val CREATED = 201
    }
}
