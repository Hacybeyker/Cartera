package com.hacybeyker.cartera.demo.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los ejemplos del contrato son válidos y, salvo los que se dicen, son lo que responde el backend embebido. */
class ContractExamplesTest {
    /** Un ejemplo que ningún dato de los fixtures puede producir, con el motivo. [operation] nulo vale para todas. */
    private class Handwritten(val operation: String?, val status: Int?, val name: String, val reason: String) {
        fun matches(example: ContractExample) = (operation == null || operation == example.operation) &&
            status == example.status &&
            name == example.name
    }

    private val handwritten = listOf(
        Handwritten(
            null,
            500,
            "errorInterno",
            "un fallo real no dice «Fallo inyectado.»; el ejemplo describe uno real"
        ),
        Handwritten("post /bill-payments", 422, "saldoInsuficiente", "los recibos de los fixtures no superan el saldo"),
        Handwritten("post /auth/biometric/enroll", 201, "dispositivo", "el deviceToken es aleatorio"),
        Handwritten("post /auth/biometric/login", null, "dispositivo", "el deviceToken es aleatorio")
    )

    private fun isHandwritten(example: ContractExample) = handwritten.any { it.matches(example) }

    @Test
    fun `every example is valid against its schema`() {
        val failures = ContractSpec.examples.flatMap { example ->
            val body = bodyOf(example)
            val errors = if (example.isRequest) {
                val headers = mapOf("Authorization" to "Bearer x", Env.IDEMPOTENCY to "key-1")
                ContractValidator.request(example.operation, headers, emptyMap(), body)
            } else {
                ContractValidator.response(example.operation, requireNotNull(example.status), example.mediaType, body)
            }
            errors.map { "$example: $it" } + undocumented(example)
        }

        assertTrue("\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `response examples are what the backend answers from clean data`() {
        val failures = ContractSpec.examples.filterNot { it.isRequest || isHandwritten(it) }.mapNotNull { example ->
            val scenario = ContractScenarios.all.firstOrNull {
                it.operation == example.operation && it.status == example.status && it.example == example.name
            } ?: return@mapNotNull "$example: ninguna prueba produce este ejemplo"
            val actual = scenario.play(Env()).response.body.orEmpty()
            "$example: difiere de lo que responde el backend".takeUnless { sameBody(example, scenario, actual) }
        }

        assertTrue("\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `every handwritten example exists in the contract`() {
        val stale = handwritten.filter { entry -> ContractSpec.examples.none(entry::matches) }

        assertTrue(stale.joinToString { "${it.operation} ${it.status} ${it.name}" }, stale.isEmpty())
    }

    @Test
    fun `request examples are accepted by the backend`() {
        val failures = ContractSpec.examples.filter { it.isRequest && !isHandwritten(it) }.mapNotNull { example ->
            val env = Env()
            if (example.operation == "post /fx/exchanges") env.quoteId()
            val path = example.operation.substringAfter(' ').removePrefix("/")
            val key = Env.IDEMPOTENCY.takeIf { path != "auth/login" }?.let { "key-1" }
            val response = env.post(path, bodyOf(example), key).response
            "$example: respondió ${response.status}: ${response.body}".takeUnless { response.status in 200..299 }
        }

        assertTrue("\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun sameBody(example: ContractExample, scenario: Scenario, actual: String): Boolean {
        val expected = bodyOf(example)
        if (example.mediaType == "text/html") return expected == actual
        return normalized(ContractSpec.json.readTree(expected)) ==
            normalized(scenario.adapt(ContractSpec.json.readTree(actual)))
    }

    private fun bodyOf(example: ContractExample): String =
        if (example.value.isTextual) example.value.asText() else ContractSpec.json.writeValueAsString(example.value)

    private fun undocumented(example: ContractExample): List<String> {
        val operation = ContractSpec.operations.getValue(example.operation)
        val status = example.status
        val schema = if (status == null) {
            operation.get("requestBody")?.let(ContractSpec::resolve)?.get("content")?.get(example.mediaType)
                ?.get("schema")
        } else {
            ContractSpec.responseSchema(operation, status, example.mediaType)
        }
        if (schema == null || example.value.isTextual) return emptyList()
        return UndocumentedFields.find(schema, example.value).map { "$example: campo sin documentar $it" }
    }

    // El identificador de la solicitud cambia con cada llamada; todo lo demás debe ser igual.
    private fun normalized(node: JsonNode): JsonNode {
        val copy = node.deepCopy<JsonNode>()
        if (copy is ObjectNode && copy.has("traceId")) copy.put("traceId", "req")
        return copy
    }
}
