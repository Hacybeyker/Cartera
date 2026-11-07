package com.hacybeyker.cartera.demo.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointAndScenarioTest {

    @Test
    fun `every endpoint resolves with its path parameter`() {
        val cases = mapOf(
            ("GET" to "app/config") to Endpoint.Match(Endpoint.APP_CONFIG, ""),
            ("POST" to "auth/login") to Endpoint.Match(Endpoint.LOGIN, ""),
            ("GET" to "me") to Endpoint.Match(Endpoint.ME, ""),
            ("GET" to "accounts") to Endpoint.Match(Endpoint.ACCOUNTS, ""),
            ("GET" to "accounts/ACC-001") to Endpoint.Match(Endpoint.ACCOUNT, "ACC-001"),
            ("GET" to "movements") to Endpoint.Match(Endpoint.MOVEMENTS, ""),
            ("GET" to "movements/MOV-1/") to Endpoint.Match(Endpoint.MOVEMENT, "MOV-1"),
            ("GET" to "movements/MOV-1/breakdown") to Endpoint.Match(Endpoint.BREAKDOWN, "MOV-1"),
            ("POST" to "transfers") to Endpoint.Match(Endpoint.TRANSFERS, ""),
            ("POST" to "_demo/reset") to Endpoint.Match(Endpoint.DEMO_RESET, "")
        )

        cases.forEach { (request, expected) ->
            assertEquals(expected, Endpoint.resolve(request.first, "${Endpoint.BASE_PATH}${request.second}"))
        }
    }

    @Test
    fun `unknown method, path or base path does not resolve`() {
        assertNull(Endpoint.resolve("POST", "/api/v1/accounts"))
        assertNull(Endpoint.resolve("GET", "/api/v1/accounts/ACC-001/extra"))
        assertNull(Endpoint.resolve("GET", "/api/v2/accounts"))
    }

    @Test
    fun `endpoint ids match the scenario channel names`() {
        assertEquals(
            listOf(
                "app_config", "login", "me", "accounts", "account", "movements", "movement", "breakdown", "transfers"
            ),
            Endpoint.entries.filter { it != Endpoint.DEMO_RESET }.map { it.id }
        )
    }

    @Test
    fun `scenario without headers has no effect`() {
        assertEquals(DemoScenario(), DemoScenario.from(BackendRequest("GET", "/")))
    }

    @Test
    fun `scenario reads and clamps every header`() {
        val request = BackendRequest(
            method = "GET",
            path = "/",
            headers = mapOf(
                "x-demo-latency-ms" to "2000",
                "X-Demo-Failure-Rate" to "150",
                "X-Demo-Failure-Mode" to "timeout",
                "X-Demo-Backend-Down" to "true"
            )
        )

        assertEquals(
            DemoScenario(latencyMs = 2000, failureRate = 100, failureMode = FailureMode.TIMEOUT, backendDown = true),
            DemoScenario.from(request)
        )
    }

    @Test
    fun `invalid scenario values fall back to no effect`() {
        val request = BackendRequest(
            method = "GET",
            path = "/",
            headers = mapOf(
                "X-Demo-Latency-Ms" to "-5",
                "X-Demo-Failure-Rate" to "mucho",
                "X-Demo-Failure-Mode" to "explode",
                "X-Demo-Backend-Down" to "yes"
            )
        )

        assertEquals(DemoScenario(), DemoScenario.from(request))
        assertEquals(FailureMode.DISCONNECT, FailureMode.fromWire("disconnect"))
    }

    @Test
    fun `failure injector is deterministic and resettable`() {
        val injector = FailureInjector()
        val first = List(50) { injector.shouldFail(30) }
        injector.reset()
        val second = List(50) { injector.shouldFail(30) }

        assertEquals(first, second)
        assertTrue(first.any { it })
        assertTrue(first.any { !it })
    }

    @Test
    fun `failure rate bounds are respected`() {
        val injector = FailureInjector()

        assertFalse(List(100) { injector.shouldFail(0) }.any { it })
        assertTrue(List(100) { injector.shouldFail(100) }.all { it })
    }
}
