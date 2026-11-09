package com.hacybeyker.cartera.demo.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

/** Un ejemplo con nombre de la petición o de una respuesta de `openapi.yaml`. */
data class ContractExample(
    val operation: String,
    val status: Int?,
    val mediaType: String,
    val name: String,
    val value: JsonNode
) {
    val isRequest: Boolean get() = status == null
    override fun toString() = "$operation ${status ?: "request"} $mediaType $name"
}

/** `openapi.yaml` como árbol, con los `$ref` locales resueltos bajo demanda. */
object ContractSpec {
    val rootDir = File(requireNotNull(System.getProperty("cartera.rootDir")))
    val openApiFile = File(rootDir, "api/openapi.yaml")
    val json = ObjectMapper()

    private val yaml = ObjectMapper(YAMLFactory())
    val document: JsonNode by lazy { yaml.readTree(openApiFile) }

    private val methods = listOf("get", "post", "put", "patch", "delete")

    fun resolve(node: JsonNode): JsonNode {
        var current = node
        while (current.has(REF)) current = document.at(current.get(REF).asText().removePrefix("#"))
        return current
    }

    /** Las operaciones con su clave `método /ruta` (por ejemplo `get /accounts`). */
    val operations: Map<String, JsonNode> by lazy {
        buildMap {
            document.get("paths").fields().forEach { (path, item) ->
                methods.filter(item::has).forEach { put("$it $path", item.get(it)) }
            }
        }
    }

    fun isAuthenticated(operation: JsonNode): Boolean {
        val security = operation.get("security") ?: document.get("security")
        return security.size() > 0
    }

    /** Los estados documentados de una operación. */
    fun statuses(operation: JsonNode): List<Int> = operation.get("responses").fieldNames().asSequence()
        .map(String::toInt).toList()

    fun responseSchema(operation: JsonNode, status: Int, mediaType: String): JsonNode? =
        content(resolve(operation.get("responses").get(status.toString())))?.get(mediaType)?.get("schema")

    val examples: List<ContractExample> by lazy {
        operations.flatMap { (key, operation) -> requestExamples(key, operation) + responseExamples(key, operation) }
    }

    private fun requestExamples(key: String, operation: JsonNode): List<ContractExample> {
        val content = operation.get("requestBody")?.let(::resolve)?.let(::content) ?: return emptyList()
        return namedExamples(key, null, content)
    }

    private fun responseExamples(key: String, operation: JsonNode): List<ContractExample> =
        operation.get("responses").fields().asSequence().flatMap { (status, response) ->
            content(resolve(response))?.let { namedExamples(key, status.toInt(), it) }.orEmpty().asSequence()
        }.toList()

    private fun namedExamples(key: String, status: Int?, content: JsonNode): List<ContractExample> =
        content.fields().asSequence().flatMap { (mediaType, media) ->
            media.get("examples")?.fields()?.asSequence().orEmpty().map { (name, example) ->
                ContractExample(key, status, mediaType, name, resolve(example).get("value"))
            }
        }.toList()

    /** La ruta de una operación con un valor cualquiera en cada `{parámetro}`. */
    fun concrete(path: String): String = path.replace(Regex("\\{[^}]+}"), "x")

    private fun content(response: JsonNode): JsonNode? = response.get("content")

    private const val REF = "\$ref"
}
