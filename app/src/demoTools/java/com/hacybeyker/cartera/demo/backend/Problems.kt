package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

enum class ProblemCode(val status: Int, val title: String) {
    INVALID_CREDENTIALS(401, "Credenciales inválidas"),
    SESSION_EXPIRED(401, "Sesión expirada"),
    UNAUTHORIZED(401, "No autorizado"),
    NOT_FOUND(404, "Recurso no encontrado"),
    VALIDATION_ERROR(422, "Datos inválidos"),
    INSUFFICIENT_FUNDS(422, "Saldo insuficiente"),
    DESTINATION_NOT_FOUND(422, "Cuenta de destino no encontrada"),
    CURRENCY_MISMATCH(422, "Las monedas no coinciden"),
    INTERNAL_ERROR(500, "Error interno"),
    SERVICE_UNAVAILABLE(503, "Servicio no disponible")
}

data class FieldError(val field: String, val code: String, val message: String)

class ProblemException(val code: ProblemCode, val detail: String, val errors: List<FieldError> = emptyList()) :
    RuntimeException(detail)

fun problem(
    code: ProblemCode,
    detail: String,
    traceId: String,
    errors: List<FieldError> = emptyList(),
    headers: Map<String, String> = emptyMap()
): BackendResponse {
    val body = buildJsonObject {
        put("type", "https://cartera.invalid/problems/${code.name.lowercase().replace('_', '-')}")
        put("title", code.title)
        put("status", code.status)
        put("detail", detail)
        put("code", code.name)
        put("traceId", traceId)
        if (errors.isNotEmpty()) {
            putJsonArray("errors") {
                errors.forEach { error ->
                    addJsonObject {
                        put("field", error.field)
                        put("code", error.code)
                        put("message", error.message)
                    }
                }
            }
        }
    }
    return BackendResponse(
        status = code.status,
        body = body.toString(),
        contentType = BackendResponse.PROBLEM_JSON,
        headers = headers
    )
}

fun validationProblem(field: String, code: String, message: String) =
    ProblemException(ProblemCode.VALIDATION_ERROR, message, listOf(FieldError(field, code, message)))

fun json(status: Int, body: JsonObject): BackendResponse = BackendResponse(status = status, body = body.toString())
