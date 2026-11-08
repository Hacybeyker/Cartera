package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class BillPaymentRequest(
    val biller: Biller,
    val customerCode: String,
    val debtIds: List<String>,
    val accountId: String,
    val pin: String
)

/** Valida la forma del cuerpo de `POST /bill-payments` y devuelve un error por campo. */
class BillPaymentParser(private val billers: (String) -> Biller?) {

    fun parse(body: JsonObject): BillPaymentRequest {
        val errors = mutableListOf<FieldError>()
        val biller = biller(body, errors)
        val customerCode = customerCode(body, biller, errors)
        val debtIds = debtIds(body, errors)
        val accountId = body.text("accountId")
        val pin = body.text("pin")
        if (accountId.isNullOrBlank()) errors += required("accountId")
        when {
            pin == null -> errors += required("pin")
            !PIN_FORMAT.matches(pin) -> errors += FieldError("pin", "INVALID_FORMAT", "Usa 4 dígitos.")
        }
        if (errors.isNotEmpty()) {
            throw ProblemException(ProblemCode.VALIDATION_ERROR, "El pago tiene datos inválidos.", errors)
        }
        return BillPaymentRequest(
            biller = requireNotNull(biller),
            customerCode = requireNotNull(customerCode),
            debtIds = debtIds,
            accountId = requireNotNull(accountId),
            pin = requireNotNull(pin)
        )
    }

    private fun biller(body: JsonObject, errors: MutableList<FieldError>): Biller? {
        val id = body.text("billerId")
        val biller = id?.let(billers)
        when {
            id.isNullOrBlank() -> errors += required("billerId")
            biller == null -> errors += FieldError("billerId", "UNKNOWN_BILLER", "No existe una empresa con ese id.")
        }
        return biller
    }

    private fun customerCode(body: JsonObject, biller: Biller?, errors: MutableList<FieldError>): String? {
        val code = body.text("customerCode")
        when {
            code.isNullOrBlank() -> errors += required("customerCode")
            biller != null && !biller.accepts(code) ->
                errors += FieldError("customerCode", "INVALID_FORMAT", "El código no cumple el formato de la empresa.")
        }
        return code
    }

    private fun debtIds(body: JsonObject, errors: MutableList<FieldError>): List<String> {
        val raw = body["debtIds"] as? JsonArray
        if (raw == null) {
            errors += required("debtIds")
            return emptyList()
        }
        val ids = raw.mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content }
        when {
            raw.isEmpty() -> errors += FieldError("debtIds", "MIN_ITEMS", "Elige al menos un recibo.")
            ids.size != raw.size || ids.any { it.isBlank() } ->
                errors += FieldError("debtIds", "INVALID_FORMAT", "Cada recibo es un id de texto.")
            ids.toSet().size != ids.size -> errors += FieldError("debtIds", "DUPLICATED", "Un recibo se repite.")
        }
        return ids
    }

    private fun required(field: String) = FieldError(field, "REQUIRED", "El campo es obligatorio.")

    private companion object {
        val PIN_FORMAT = Regex("""\d{4}""")
    }
}
