package com.hacybeyker.cartera.feature.order.domain.model

enum class OrderField {
    SYMBOL,
    SIDE,
    QUANTITY,
    ACCOUNT
}

enum class OrderFieldError {
    REQUIRED,
    INVALID_FORMAT,
    MUST_BE_POSITIVE,
    EXCEEDS_BALANCE,
    EXCEEDS_HOLDINGS,
    NOT_OWNED,
    UNSUPPORTED_CURRENCY,
    UNKNOWN_SYMBOL
}

/** Los errores por campo de un 422 `VALIDATION_ERROR`, con los nombres de campo y códigos del contrato. */
fun orderFieldErrors(fieldCodes: Map<String, String>): Map<OrderField, OrderFieldError> =
    fieldCodes.entries.mapNotNull { (field, code) ->
        contractField(field)?.let { it to contractCode(code) }
    }.toMap()

private fun contractField(field: String): OrderField? = when (field) {
    "symbol" -> OrderField.SYMBOL
    "side" -> OrderField.SIDE
    "quantity" -> OrderField.QUANTITY
    "accountId" -> OrderField.ACCOUNT
    else -> null
}

private fun contractCode(code: String): OrderFieldError =
    OrderFieldError.entries.firstOrNull { it.name == code } ?: OrderFieldError.INVALID_FORMAT
