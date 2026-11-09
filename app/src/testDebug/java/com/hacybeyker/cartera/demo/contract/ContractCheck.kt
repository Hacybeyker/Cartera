package com.hacybeyker.cartera.demo.contract

import com.fasterxml.jackson.databind.JsonNode

/** Las comprobaciones de una respuesta del backend contra el contrato, como una lista de problemas legibles. */
object ContractCheck {
    private const val UTF8 = "; charset=utf-8"

    fun problems(scenario: Scenario, played: Played): List<String> {
        val response = played.response
        val label = scenario.toString()
        val status = if (response.status == scenario.status) {
            emptyList()
        } else {
            listOf("$label: respondió ${response.status}: ${response.body}")
        }
        val contentType = response.body?.let { response.contentType + UTF8 }
        val schema = contentType?.let { ContractSpec.operations.getValue(scenario.operation) }
            ?.let { ContractSpec.responseSchema(it, response.status, response.contentType) }
        val body = response.body
        return status +
            ContractValidator.response(scenario.operation, response.status, contentType, body).map { "$label: $it" } +
            undocumented(label, schema, response.contentType, body) +
            requestProblems(label, scenario, played)
    }

    private fun undocumented(label: String, schema: JsonNode?, contentType: String, body: String?): List<String> {
        if (schema == null || body == null || !contentType.endsWith("json")) return emptyList()
        return UndocumentedFields.find(schema, ContractSpec.json.readTree(body)).map {
            "$label: campo sin documentar $it"
        }
    }

    // Solo las peticiones que dieron una respuesta de éxito deben ser válidas; las de error se mandan rotas a propósito.
    private fun requestProblems(label: String, scenario: Scenario, played: Played): List<String> {
        if (played.response.status !in SUCCESS || played.sent.path.startsWith("_demo")) return emptyList()
        val headers = played.sent.headers + ("Authorization" to "Bearer x")
        return ContractValidator.request(scenario.operation, headers, played.sent.query, played.sent.body)
            .map { "$label (petición): $it" }
    }

    private val SUCCESS = 200..299
}
