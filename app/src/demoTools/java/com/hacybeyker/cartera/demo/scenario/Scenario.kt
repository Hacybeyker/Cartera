package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.demo.backend.DemoScenario
import com.hacybeyker.cartera.demo.backend.FailureMode

data class EndpointValues<T>(val default: T, val overrides: Map<String, T> = emptyMap()) {
    fun valueFor(endpointId: String): T = overrides[endpointId] ?: default

    fun encode(): String {
        val entries = listOf("default=$default") + overrides.map { (id, value) -> "$id=$value" }
        return entries.joinToString(",")
    }
}

data class Scenario(
    val latencyMs: EndpointValues<Long> = EndpointValues(DEFAULT_LATENCY_MS),
    val failureRate: EndpointValues<Int> = EndpointValues(0),
    val failureMode: FailureMode = FailureMode.HTTP_500,
    val sessionTtlSeconds: Long = DEFAULT_SESSION_TTL_SECONDS,
    val backendDown: Boolean = false,
    val tickMs: Int = DemoScenario.DEFAULT_TICK_MS,
    val streamDropSeconds: Int = 0,
    val quoteTtlSeconds: Int = DemoScenario.DEFAULT_QUOTE_TTL_SECONDS,
    /** Semilla del orden del teclado de la clave; es del cliente y no viaja al backend. 0 = orden fijo 1–9, 0. */
    val pinPadSeed: Int = 0,
    /** Interruptores del cliente encendidos; no viajan al backend. */
    val flags: Set<FeatureFlag> = emptySet()
) {
    fun toValues(): Map<String, String> = mapOf(
        ScenarioKeys.LATENCY to latencyMs.encode(),
        ScenarioKeys.FAILURE_RATE to failureRate.encode(),
        ScenarioKeys.FAILURE_MODE to failureMode.wireName,
        ScenarioKeys.SESSION_TTL to sessionTtlSeconds.toString(),
        ScenarioKeys.BACKEND_DOWN to backendDown.toString(),
        ScenarioKeys.TICK_MS to tickMs.toString(),
        ScenarioKeys.STREAM_DROP to streamDropSeconds.toString(),
        ScenarioKeys.QUOTE_TTL to quoteTtlSeconds.toString(),
        ScenarioKeys.PIN_PAD_SEED to pinPadSeed.toString()
    ) + FeatureFlag.entries.associate { it.key to (it in flags).toString() }

    fun describe(): String = toValues().entries.joinToString(" ") { (key, value) -> "$key=$value" }

    companion object {
        const val DEFAULT_LATENCY_MS = 400L
        const val DEFAULT_SESSION_TTL_SECONDS = 900L
    }
}

object ScenarioKeys {
    const val LATENCY = "latency"
    const val FAILURE_RATE = "failure_rate"
    const val FAILURE_MODE = "failure_mode"
    const val SESSION_TTL = "session_ttl"
    const val BACKEND_DOWN = "backend_down"
    const val TICK_MS = "tick_ms"
    const val STREAM_DROP = "stream_drop_s"
    const val QUOTE_TTL = "quote_ttl_s"
    const val PIN_PAD_SEED = "pin_pad_seed"
    const val DEFAULTS = "defaults"
}
