package com.hacybeyker.cartera.feature.transfer.domain.model

enum class TransferField {
    SOURCE,
    DESTINATION,
    AMOUNT,
    DESCRIPTION
}

enum class TransferFieldError {
    REQUIRED,
    INVALID_FORMAT,
    MUST_BE_POSITIVE,
    EXCEEDS_BALANCE,
    TOO_LONG,
    NOT_OWNED
}

/** Los errores por campo de un 422 `VALIDATION_ERROR`, con los nombres de campo y códigos del contrato. */
fun transferFieldErrors(fieldCodes: Map<String, String>): Map<TransferField, TransferFieldError> =
    fieldCodes.entries.mapNotNull { (field, code) ->
        contractField(field)?.let { it to contractCode(code) }
    }.toMap()

private fun contractField(field: String): TransferField? = when (field) {
    "sourceAccountId" -> TransferField.SOURCE
    "destinationAccountNumber" -> TransferField.DESTINATION
    "amount.value", "amount.currency" -> TransferField.AMOUNT
    "description" -> TransferField.DESCRIPTION
    else -> null
}

private fun contractCode(code: String): TransferFieldError =
    TransferFieldError.entries.firstOrNull { it.name == code } ?: TransferFieldError.INVALID_FORMAT
