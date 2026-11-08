package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.demo.backend.DemoScenario
import com.hacybeyker.cartera.demo.backend.Endpoint
import okhttp3.Interceptor
import okhttp3.Response

// El cliente resuelve el valor de cada endpoint y lo envía; el backend solo honra la cabecera.
class DemoScenarioInterceptor(private val store: ScenarioStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val endpoint = Endpoint.resolve(request.method, request.url.encodedPath)?.endpoint
        if (endpoint == null || endpoint == Endpoint.DEMO_RESET) return chain.proceed(request)
        val scenario = store.load()
        val builder = request.newBuilder()
            .header(DemoScenario.LATENCY_HEADER, scenario.latencyMs.valueFor(endpoint.id).toString())
            .header(DemoScenario.FAILURE_RATE_HEADER, scenario.failureRate.valueFor(endpoint.id).toString())
            .header(DemoScenario.FAILURE_MODE_HEADER, scenario.failureMode.wireName)
        if (scenario.backendDown) builder.header(DemoScenario.BACKEND_DOWN_HEADER, "true")
        parameterHeaders(endpoint, scenario).forEach { (name, value) -> builder.header(name, value) }
        return chain.proceed(builder.build())
    }

    // Las cabeceras que fijan un parámetro viajan solo con el endpoint que las honra.
    private fun parameterHeaders(endpoint: Endpoint, scenario: Scenario): Map<String, String> = when (endpoint) {
        Endpoint.LOGIN, Endpoint.BIOMETRIC_LOGIN ->
            mapOf(DemoScenario.SESSION_TTL_HEADER to scenario.sessionTtlSeconds.toString())
        Endpoint.FX_RATES -> mapOf(DemoScenario.QUOTE_TTL_HEADER to scenario.quoteTtlSeconds.toString())
        Endpoint.MARKET_STREAM -> mapOf(
            DemoScenario.TICK_MS_HEADER to scenario.tickMs.toString(),
            DemoScenario.STREAM_DROP_HEADER to scenario.streamDropSeconds.toString(),
            DemoScenario.QUOTE_TTL_HEADER to scenario.quoteTtlSeconds.toString()
        )
        else -> emptyMap()
    }
}
