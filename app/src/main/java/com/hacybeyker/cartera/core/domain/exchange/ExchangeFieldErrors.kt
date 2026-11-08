package com.hacybeyker.cartera.core.domain.exchange

enum class ExchangeField {
    AMOUNT,
    SOURCE,
    DESTINATION,
    QUOTE
}

enum class ExchangeFieldError {
    REQUIRED,
    INVALID_FORMAT,
    MUST_BE_POSITIVE,
    EXCEEDS_BALANCE,

    /** El monto no alcanza para un centavo de la otra moneda. */
    TOO_SMALL,
    NOT_OWNED,

    /** La cotización no existe (por ejemplo, después de restablecer los datos). */
    NOT_FOUND,

    /** La cuenta no es de la moneda que pide el sentido del cambio. */
    WRONG_CURRENCY
}

/** Los errores por campo de un 422 `VALIDATION_ERROR`, con los nombres de campo y códigos del contrato. */
fun exchangeFieldErrors(fieldCodes: Map<String, String>): Map<ExchangeField, ExchangeFieldError> =
    fieldCodes.entries.mapNotNull { (field, code) ->
        contractField(field)?.let { it to contractCode(code) }
    }.toMap()

private fun contractField(field: String): ExchangeField? = when (field) {
    "amount.value", "amount.currency" -> ExchangeField.AMOUNT
    "sourceAccountId" -> ExchangeField.SOURCE
    "destinationAccountId" -> ExchangeField.DESTINATION
    "quoteId" -> ExchangeField.QUOTE
    else -> null
}

private fun contractCode(code: String): ExchangeFieldError =
    ExchangeFieldError.entries.firstOrNull { it.name == code } ?: ExchangeFieldError.INVALID_FORMAT
