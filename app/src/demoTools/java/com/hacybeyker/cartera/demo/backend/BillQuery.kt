package com.hacybeyker.cartera.demo.backend

import java.text.Normalizer

/** Lo que se pide en `GET /billers/{billerId}/debts`, ya validado. */
data class DebtsQuery(val customerCode: String, val includeHistory: Boolean) {
    companion object {
        fun parse(biller: Biller, query: Map<String, String>): DebtsQuery {
            val customerCode = query["customerCode"].orEmpty()
            val history = query["includeHistory"]
            val errors = listOfNotNull(
                customerCodeError(biller, customerCode),
                FieldError("includeHistory", "INVALID_VALUE", "Usa true o false.")
                    .takeIf { history !in HISTORY_VALUES }
            )
            if (errors.isNotEmpty()) {
                throw ProblemException(ProblemCode.VALIDATION_ERROR, "La consulta tiene datos inválidos.", errors)
            }
            return DebtsQuery(customerCode, includeHistory = history == "true")
        }

        private fun customerCodeError(biller: Biller, customerCode: String): FieldError? = when {
            customerCode.isBlank() -> FieldError("customerCode", "REQUIRED", "El campo es obligatorio.")
            !biller.accepts(customerCode) ->
                FieldError("customerCode", "INVALID_FORMAT", "El código no cumple el formato de la empresa.")
            else -> null
        }

        private val HISTORY_VALUES = setOf(null, "", "true", "false")
    }
}

/** Búsqueda de empresas por nombre o rubro. */
object BillerSearch {
    private val DIACRITICS = Regex("\\p{Mn}+")

    /** Cada palabra del texto debe aparecer en el nombre o en el rubro, sin mayúsculas ni tildes. */
    fun filter(billers: List<Biller>, text: String): List<Biller> {
        val terms = normalize(text).split(' ').filter { it.isNotEmpty() }
        return billers.filter { biller ->
            val haystack = normalize("${biller.name} ${biller.category}")
            terms.all { it in haystack }
        }
    }

    private fun normalize(text: String): String = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()
}
