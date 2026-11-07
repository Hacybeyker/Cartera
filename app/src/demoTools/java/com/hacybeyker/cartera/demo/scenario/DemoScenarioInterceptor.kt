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
        if (endpoint == Endpoint.LOGIN) {
            builder.header(DemoScenario.SESSION_TTL_HEADER, scenario.sessionTtlSeconds.toString())
        }
        return chain.proceed(builder.build())
    }
}
