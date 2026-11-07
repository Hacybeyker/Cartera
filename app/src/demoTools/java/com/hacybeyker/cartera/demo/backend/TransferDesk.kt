package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class TransferOrder(
    val sourceAccountId: String,
    val destinationAccountNumber: String,
    val amount: BigDecimal,
    val currency: String,
    val description: String?
)

class TransferDesk(private val state: BackendState, private val clock: Clock) {

    fun transfer(userId: String, idempotencyKey: String?, body: JsonObject): BackendResponse {
        if (idempotencyKey.isNullOrBlank()) {
            reject(FieldError(IDEMPOTENCY_HEADER, "REQUIRED", "La cabecera es obligatoria."))
        }
        val storageKey = "$userId:$idempotencyKey"
        return state.idempotency[storageKey] ?: transferOnce(userId, parse(body)).also {
            state.idempotency[storageKey] = it
        }
    }

    private fun transferOnce(userId: String, order: TransferOrder): BackendResponse {
        val source = state.account(userId, order.sourceAccountId)
            ?: reject(FieldError("sourceAccountId", "NOT_OWNED", "La cuenta de origen no es tuya."))
        val destination = state.thirdPartyAccount(order.destinationAccountNumber)
            ?: reject(ProblemCode.DESTINATION_NOT_FOUND, "No existe una cuenta con ese número.")
        val currency = source.string("currency")
        if (destination.string("currency") != currency || order.currency != currency) {
            reject(ProblemCode.CURRENCY_MISMATCH, "El origen, el destino y el monto deben usar $currency.")
        }
        val available = source.availableBalance()
        if (order.amount > available) {
            reject(ProblemCode.INSUFFICIENT_FUNDS, "El monto supera el saldo disponible.")
        }
        return execute(order, destination.string("holderName"), available - order.amount)
    }

    private fun execute(order: TransferOrder, holderName: String, newBalance: BigDecimal): BackendResponse {
        val sequence = state.nextTransferSequence()
        val createdAt = OffsetDateTime.ofInstant(clock.instant(), LIMA).truncatedTo(ChronoUnit.SECONDS)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val movementId = "MOV-${order.sourceAccountId.substringAfter("ACC-")}-T%04d".format(sequence)
        val operationNumber = "9%07d".format(sequence)
        val movement = movementJson(order, movementId, operationNumber, createdAt, holderName)
        state.debit(order.sourceAccountId, newBalance, MovementRecord.from(movement))
        return json(
            status = CREATED,
            body = buildJsonObject {
                put("transferId", "TRF-%06d".format(sequence))
                put("operationNumber", operationNumber)
                put("createdAt", createdAt)
                put("sourceAccountId", order.sourceAccountId)
                put("sourceAvailableBalance", money(newBalance, order.currency))
                put("movementId", movementId)
            }
        )
    }

    private fun movementJson(
        order: TransferOrder,
        movementId: String,
        operationNumber: String,
        createdAt: String,
        holderName: String
    ): JsonObject = buildJsonObject {
        put("id", movementId)
        put("accountId", order.sourceAccountId)
        put("date", createdAt)
        put("description", order.description ?: "Transferencia a $holderName")
        put("category", "TRANSFER")
        put("amount", money(order.amount.negate(), order.currency))
        put("type", "DEBIT")
        put("operationNumber", operationNumber)
        put("channel", "APP")
        put("status", "COMPLETED")
        putJsonObject("counterparty") {
            put("name", holderName)
            put("reference", order.destinationAccountNumber)
        }
        putJsonObject("breakdown") {
            putJsonArray("lines") {
                addJsonObject {
                    put("concept", "Transferencia")
                    put("amount", money(order.amount, order.currency))
                }
            }
            put("fees", money(BigDecimal.ZERO, order.currency))
            put("taxes", money(BigDecimal.ZERO, order.currency))
            put("merchant", JsonNull)
            put("category", "TRANSFER")
        }
    }

    private fun parse(body: JsonObject): TransferOrder {
        val errors = mutableListOf<FieldError>()
        val source = body.text("sourceAccountId")
        val destination = body.text("destinationAccountNumber")
        val amount = body["amount"] as? JsonObject
        val value = amount?.text("value")
        val currency = amount?.text("currency")
        val description = body.text("description")
        if (source.isNullOrBlank()) errors += required("sourceAccountId")
        if (destination.isNullOrBlank()) errors += required("destinationAccountNumber")
        if (value == null || !AMOUNT_FORMAT.matches(value)) {
            errors += FieldError("amount.value", "INVALID_FORMAT", "Usa un decimal con hasta dos decimales.")
        } else if (BigDecimal(value).signum() <= 0) {
            errors += FieldError("amount.value", "MUST_BE_POSITIVE", "El monto debe ser mayor que cero.")
        }
        if (currency == null || !CURRENCY_FORMAT.matches(currency)) {
            errors += FieldError("amount.currency", "INVALID_FORMAT", "Usa un código ISO 4217.")
        }
        if (description != null && description.length > MAX_DESCRIPTION) {
            errors += FieldError("description", "TOO_LONG", "Máximo $MAX_DESCRIPTION caracteres.")
        }
        if (errors.isNotEmpty()) reject(*errors.toTypedArray())
        return TransferOrder(
            sourceAccountId = requireNotNull(source),
            destinationAccountNumber = requireNotNull(destination),
            amount = BigDecimal(value).setScale(2),
            currency = requireNotNull(currency),
            description = description?.takeIf { it.isNotBlank() }
        )
    }

    private fun required(field: String) = FieldError(field, "REQUIRED", "El campo es obligatorio.")

    private fun reject(vararg errors: FieldError): Nothing =
        throw ProblemException(ProblemCode.VALIDATION_ERROR, "La transferencia tiene datos inválidos.", errors.toList())

    private fun reject(code: ProblemCode, detail: String): Nothing = throw ProblemException(code, detail)

    private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

    companion object {
        const val IDEMPOTENCY_HEADER = "Idempotency-Key"
        private const val CREATED = 201
        private const val MAX_DESCRIPTION = 140
        private val LIMA = ZoneOffset.ofHours(-5)
        private val AMOUNT_FORMAT = Regex("""\d{1,9}(\.\d{1,2})?""")
        private val CURRENCY_FORMAT = Regex("[A-Z]{3}")
    }
}
