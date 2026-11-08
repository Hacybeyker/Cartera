package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

data class BackendRequest(
    val method: String,
    val path: String,
    val query: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
    val requestId: String = "req-test"
) {
    fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    fun jsonBody(): JsonObject = try {
        Json.parseToJsonElement(body.ifBlank { "{}" }).jsonObject
    } catch (_: IllegalArgumentException) {
        throw validationProblem("body", "INVALID_JSON", "El cuerpo debe ser un objeto JSON.")
    }
}

data class BackendResponse(
    val status: Int,
    val body: String? = null,
    val contentType: String = JSON,
    val headers: Map<String, String> = emptyMap()
) {
    companion object {
        const val JSON = "application/json"
        const val PROBLEM_JSON = "application/problem+json"
    }
}

fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
