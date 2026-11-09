package com.hacybeyker.cartera.demo.contract

import com.fasterxml.jackson.databind.JsonNode

/**
 * Los campos de un cuerpo que su esquema no declara. Reúne las propiedades de `allOf`, `oneOf` y `anyOf`, porque el
 * validador trata todo objeto como cerrado y por eso rechaza, en falso, las composiciones (`MovementDetail`).
 */
object UndocumentedFields {
    fun find(schema: JsonNode, value: JsonNode, at: String = "$"): List<String> {
        val parts = parts(schema)
        return when {
            value.isObject -> objectProblems(parts, value, at)
            value.isArray -> parts.mapNotNull { it.get("items") }.flatMap { items ->
                value.mapIndexed { index, element -> find(items, element, "$at[$index]") }.flatten()
            }
            else -> emptyList()
        }
    }

    private fun objectProblems(parts: List<JsonNode>, value: JsonNode, at: String): List<String> {
        val declared = parts.flatMap { part -> part.get("properties")?.fieldNames()?.asSequence().orEmpty() }.toSet()
        val open = parts.any { part -> part.get("additionalProperties")?.let { it.isObject || it.asBoolean() } == true }
        val unknown = if (open) emptyList() else value.fieldNames().asSequence().filterNot(declared::contains).toList()
        val nested = value.fields().asSequence().flatMap { (name, child) ->
            parts.mapNotNull { it.get("properties")?.get(name) }.flatMap { find(it, child, "$at.$name") }.asSequence()
        }
        return unknown.map { "$at.$it" } + nested
    }

    /** El esquema, sus miembros de `allOf` y las alternativas de `oneOf` y `anyOf`, con los `$ref` resueltos. */
    private fun parts(schema: JsonNode): List<JsonNode> {
        val resolved = ContractSpec.resolve(schema)
        val members = listOf("allOf", "oneOf", "anyOf").flatMap { keyword ->
            resolved.get(keyword)?.toList().orEmpty().flatMap(::parts)
        }
        return listOf(resolved) + members
    }
}
