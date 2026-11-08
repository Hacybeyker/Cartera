package com.hacybeyker.cartera.legacy.customer.domain

import java.util.regex.PatternSyntaxException
import javax.inject.Inject

enum class CodeCheck { VALID, EMPTY, INVALID_FORMAT }

class ValidateCustomerCodeUseCase @Inject constructor() {

    /** [format] es la expresión regular que publica la empresa. */
    operator fun invoke(code: String, format: String): CodeCheck = when {
        code.isEmpty() -> CodeCheck.EMPTY
        matches(code, format) -> CodeCheck.VALID
        else -> CodeCheck.INVALID_FORMAT
    }

    // Una expresión que esta versión de Java no entiende no puede bloquear el pago: decide el servidor.
    private fun matches(code: String, format: String): Boolean = try {
        Regex(format).matches(code)
    } catch (_: PatternSyntaxException) {
        true
    }
}
