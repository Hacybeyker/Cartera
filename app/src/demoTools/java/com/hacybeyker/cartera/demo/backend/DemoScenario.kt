package com.hacybeyker.cartera.demo.backend

import kotlin.random.Random

enum class FailureMode(val wireName: String) {
    HTTP_500("http_500"),
    TIMEOUT("timeout"),
    DISCONNECT("disconnect");

    companion object {
        fun fromWire(value: String?): FailureMode = entries.firstOrNull { it.wireName == value } ?: HTTP_500
    }
}

// El servidor solo honra lo que llega por cabecera: un valor ausente o inválido equivale a "sin efecto".
data class DemoScenario(
    val latencyMs: Long = 0,
    val failureRate: Int = 0,
    val failureMode: FailureMode = FailureMode.HTTP_500,
    val backendDown: Boolean = false
) {
    companion object {
        const val LATENCY_HEADER = "X-Demo-Latency-Ms"
        const val FAILURE_RATE_HEADER = "X-Demo-Failure-Rate"
        const val FAILURE_MODE_HEADER = "X-Demo-Failure-Mode"
        const val SESSION_TTL_HEADER = "X-Demo-Session-Ttl"
        const val BACKEND_DOWN_HEADER = "X-Demo-Backend-Down"
        private const val MAX_RATE = 100

        fun from(request: BackendRequest): DemoScenario = DemoScenario(
            latencyMs = request.header(LATENCY_HEADER)?.toLongOrNull()?.coerceAtLeast(0) ?: 0,
            failureRate = request.header(FAILURE_RATE_HEADER)?.toIntOrNull()?.coerceIn(0, MAX_RATE) ?: 0,
            failureMode = FailureMode.fromWire(request.header(FAILURE_MODE_HEADER)),
            backendDown = request.header(BACKEND_DOWN_HEADER).toBoolean()
        )
    }
}

// Semilla fija: con la misma secuencia de llamadas, los fallos caen siempre en las mismas.
class FailureInjector(private val seed: Long = SEED) {
    private var random = Random(seed)

    fun shouldFail(rate: Int): Boolean = rate > 0 && random.nextInt(MAX_RATE) < rate

    fun reset() {
        random = Random(seed)
    }

    private companion object {
        const val SEED = 20261107L
        const val MAX_RATE = 100
    }
}
