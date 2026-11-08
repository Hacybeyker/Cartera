package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.demo.backend.Endpoint
import com.hacybeyker.cartera.demo.backend.FailureMode

data class ScenarioUpdate(val scenario: Scenario, val rejected: List<String>, val ignoredKeys: Set<String>) {
    fun describe(): String = buildString {
        append("Escenario efectivo: ").append(scenario.describe())
        if (rejected.isNotEmpty()) append(" | valores inválidos sin aplicar: ").append(rejected.joinToString())
        if (ignoredKeys.isNotEmpty()) append(" | claves ignoradas: ").append(ignoredKeys.sorted().joinToString())
    }
}

// Lo que no llega no cambia. Un valor inválido se rechaza y deja el anterior; una clave desconocida se ignora.
object ScenarioParser {
    private val KNOWN_KEYS = setOf(
        ScenarioKeys.LATENCY,
        ScenarioKeys.FAILURE_RATE,
        ScenarioKeys.FAILURE_MODE,
        ScenarioKeys.SESSION_TTL,
        ScenarioKeys.BACKEND_DOWN,
        ScenarioKeys.TICK_MS,
        ScenarioKeys.STREAM_DROP,
        ScenarioKeys.QUOTE_TTL,
        ScenarioKeys.DEFAULTS
    ) + FeatureFlag.entries.map { it.key }
    private val ENDPOINT_IDS = Endpoint.entries.filter { it != Endpoint.DEMO_RESET }.map { it.id }.toSet()
    private const val DEFAULT_ID = "default"
    private const val MAX_RATE = 100

    fun apply(current: Scenario, values: Map<String, Any?>): ScenarioUpdate {
        val base = if (values[ScenarioKeys.DEFAULTS]?.let(::bool) == true) Scenario() else current
        val rejected = mutableListOf<String>()

        fun <T> field(key: String, fallback: T, parse: (Any) -> T?): T {
            val raw = values[key] ?: return fallback
            return parse(raw) ?: fallback.also { rejected += "$key=$raw" }
        }

        val builtIn = Scenario()
        val scenario = Scenario(
            latencyMs = field(ScenarioKeys.LATENCY, base.latencyMs) { raw ->
                endpointValues(raw, builtIn.latencyMs.default, ::latency)
            },
            failureRate = field(ScenarioKeys.FAILURE_RATE, base.failureRate) { raw ->
                endpointValues(raw, builtIn.failureRate.default, ::rate)
            },
            failureMode = field(ScenarioKeys.FAILURE_MODE, base.failureMode) { raw ->
                FailureMode.entries.firstOrNull { it.wireName == raw.toString().trim() }
            },
            sessionTtlSeconds = field(ScenarioKeys.SESSION_TTL, base.sessionTtlSeconds) { raw ->
                raw.toString().trim().toLongOrNull()?.takeIf { it > 0 }
            },
            backendDown = field(ScenarioKeys.BACKEND_DOWN, base.backendDown, ::bool),
            tickMs = field(ScenarioKeys.TICK_MS, base.tickMs) { raw -> integer(raw)?.takeIf { it >= 0 } },
            streamDropSeconds = field(ScenarioKeys.STREAM_DROP, base.streamDropSeconds) { raw ->
                integer(raw)?.takeIf { it >= 0 }
            },
            quoteTtlSeconds = field(ScenarioKeys.QUOTE_TTL, base.quoteTtlSeconds) { raw ->
                integer(raw)?.takeIf { it > 0 }
            },
            flags = FeatureFlag.entries.filter { flag -> field(flag.key, flag in base.flags, ::bool) }.toSet()
        )
        return ScenarioUpdate(scenario, rejected, values.keys - KNOWN_KEYS)
    }

    private fun integer(raw: Any): Int? = raw.toString().trim().toIntOrNull()

    private fun latency(text: String): Long? = text.toLongOrNull()?.takeIf { it >= 0 }

    private fun rate(text: String): Int? = text.toIntOrNull()?.takeIf { it in 0..MAX_RATE }

    private fun bool(raw: Any): Boolean? = raw as? Boolean ?: raw.toString().trim().lowercase().toBooleanStrictOrNull()

    // Formato "default=400,movements=2000"; un valor sin id es el global. Sin global, rige el de fábrica.
    private fun <T> endpointValues(raw: Any, builtInDefault: T, parseValue: (String) -> T?): EndpointValues<T>? {
        val parsed = raw.toString().split(',').associate { entry ->
            val id = if ('=' in entry) entry.substringBefore('=').trim() else DEFAULT_ID
            id to parseValue(entry.substringAfter('=').trim())
        }
        val valid = parsed.entries
            .filter { (id, _) -> id == DEFAULT_ID || id in ENDPOINT_IDS }
            .mapNotNull { (id, value) -> value?.let { id to it } }
            .toMap()
        val complete = valid.size == parsed.size
        return if (complete) EndpointValues(valid[DEFAULT_ID] ?: builtInDefault, valid - DEFAULT_ID) else null
    }
}
