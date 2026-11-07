package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.demo.backend.FailureMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeScenarioStorage(var values: Map<String, String> = emptyMap()) : ScenarioStorage {
    override fun read(): Map<String, String> = values

    override fun write(values: Map<String, String>) {
        this.values = values
    }
}

class ScenarioTest {

    @Test
    fun `defaults match the documented values`() {
        assertEquals(
            "latency=default=400 failure_rate=default=0 failure_mode=http_500 session_ttl=900 backend_down=false",
            Scenario().describe()
        )
    }

    @Test
    fun `adb extras with their own types update every field`() {
        val update = ScenarioParser.apply(
            Scenario(),
            mapOf(
                "latency" to "default=400,movements=2000",
                "failure_rate" to "default=0,transfers=100",
                "failure_mode" to "timeout",
                "session_ttl" to 60,
                "backend_down" to true
            )
        )

        val scenario = update.scenario
        assertEquals(2000L, scenario.latencyMs.valueFor("movements"))
        assertEquals(400L, scenario.latencyMs.valueFor("accounts"))
        assertEquals(100, scenario.failureRate.valueFor("transfers"))
        assertEquals(FailureMode.TIMEOUT, scenario.failureMode)
        assertEquals(60L, scenario.sessionTtlSeconds)
        assertTrue(scenario.backendDown)
        assertTrue(update.rejected.isEmpty())
    }

    @Test
    fun `launch arguments arrive as text and are parsed the same way`() {
        val scenario = ScenarioParser.apply(
            Scenario(),
            mapOf("latency" to "1500", "session_ttl" to "120", "backend_down" to "TRUE", "failure_rate" to 30)
        ).scenario

        assertEquals(EndpointValues(1500L), scenario.latencyMs)
        assertEquals(120L, scenario.sessionTtlSeconds)
        assertTrue(scenario.backendDown)
        assertEquals(30, scenario.failureRate.default)
    }

    @Test
    fun `what is not sent does not change`() {
        val current = Scenario(sessionTtlSeconds = 60, backendDown = true)

        val scenario = ScenarioParser.apply(current, mapOf("failure_mode" to "disconnect")).scenario

        assertEquals(current.copy(failureMode = FailureMode.DISCONNECT), scenario)
    }

    @Test
    fun `defaults action restores before applying the rest`() {
        val current = Scenario(sessionTtlSeconds = 60, backendDown = true)

        val scenario = ScenarioParser.apply(current, mapOf("defaults" to true, "latency" to "0")).scenario

        assertEquals(Scenario(latencyMs = EndpointValues(0L)), scenario)
    }

    @Test
    fun `overrides without default keep the factory default`() {
        val current = Scenario(latencyMs = EndpointValues(900L))

        val scenario = ScenarioParser.apply(current, mapOf("latency" to "movements=2000")).scenario

        assertEquals(EndpointValues(400L, mapOf("movements" to 2000L)), scenario.latencyMs)
    }

    @Test
    fun `invalid values are rejected and keep the previous one`() {
        val update = ScenarioParser.apply(
            Scenario(),
            mapOf(
                "latency" to "default=-1",
                "failure_rate" to "cards=50",
                "failure_mode" to "explode",
                "session_ttl" to 0,
                "backend_down" to "si"
            )
        )

        assertEquals(Scenario(), update.scenario)
        assertEquals(5, update.rejected.size)
        assertTrue(update.describe().contains("valores inválidos sin aplicar: latency=default=-1"))
    }

    @Test
    fun `failure rate above one hundred is rejected`() {
        assertEquals(
            listOf("failure_rate=101"),
            ScenarioParser.apply(Scenario(), mapOf("failure_rate" to "101")).rejected
        )
    }

    @Test
    fun `unknown keys are ignored and reported`() {
        val update = ScenarioParser.apply(Scenario(), mapOf("some_flag" to true, "latency" to "10"))

        assertEquals(setOf("some_flag"), update.ignoredKeys)
        assertTrue(update.describe().endsWith("claves ignoradas: some_flag"))
    }

    @Test
    fun `store starts on defaults, saves updates and reads them back`() {
        val storage = FakeScenarioStorage()
        val store = ScenarioStore(storage)

        assertEquals(Scenario(), store.load())
        store.update(mapOf("latency" to "default=100,movements=2000", "session_ttl" to 60))

        assertEquals("default=100,movements=2000", storage.values["latency"])
        assertEquals(EndpointValues(100L, mapOf("movements" to 2000L)), store.load().latencyMs)
        assertEquals(60L, ScenarioStore(storage).load().sessionTtlSeconds)
    }

    @Test
    fun `corrupted stored values fall back to defaults`() {
        val store = ScenarioStore(FakeScenarioStorage(mapOf("latency" to "x", "failure_mode" to "timeout")))

        assertEquals(Scenario(failureMode = FailureMode.TIMEOUT), store.load())
    }
}
