package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
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
            "latency=default=400 failure_rate=default=0 failure_mode=http_500 session_ttl=900 backend_down=false " +
                "tick_ms=2000 stream_drop_s=0 quote_ttl_s=30 pin_pad_seed=0 movements_full_page=false " +
                "accounts_eager_refresh=false " +
                "breakdown_rich_transition=false quotes_full_refresh=false chart_live_path=false",
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
    fun `market keys arrive as adb integers or launch text`() {
        val fromAdb = ScenarioParser.apply(Scenario(), mapOf("tick_ms" to 0, "stream_drop_s" to 15, "quote_ttl_s" to 5))
        val fromLaunch = ScenarioParser.apply(Scenario(), mapOf("tick_ms" to "250", "quote_ttl_s" to " 60 "))

        assertEquals(Scenario(tickMs = 0, streamDropSeconds = 15, quoteTtlSeconds = 5), fromAdb.scenario)
        assertEquals(Scenario(tickMs = 250, quoteTtlSeconds = 60), fromLaunch.scenario)
        assertTrue(fromAdb.rejected.isEmpty() && fromAdb.ignoredKeys.isEmpty())
    }

    @Test
    fun `invalid market values are rejected and keep the previous one`() {
        val current = Scenario(tickMs = 0, streamDropSeconds = 15, quoteTtlSeconds = 5)

        val update = ScenarioParser.apply(
            current,
            mapOf("tick_ms" to -1, "stream_drop_s" to "nunca", "quote_ttl_s" to 0)
        )

        assertEquals(current, update.scenario)
        assertEquals(listOf("tick_ms=-1", "stream_drop_s=nunca", "quote_ttl_s=0"), update.rejected)
        assertEquals(Scenario(), ScenarioParser.apply(current, mapOf("defaults" to true)).scenario)
    }

    @Test
    fun `market values are stored and read back`() {
        val storage = FakeScenarioStorage()

        ScenarioStore(storage).update(mapOf("tick_ms" to 0, "quote_ttl_s" to 5))

        assertEquals("0", storage.values["tick_ms"])
        assertEquals("0", storage.values["stream_drop_s"])
        assertEquals("5", storage.values["quote_ttl_s"])
        assertEquals(Scenario(tickMs = 0, quoteTtlSeconds = 5), ScenarioStore(storage).load())
    }

    @Test
    fun `pin pad seed arrives as an adb integer or launch text`() {
        val fromAdb = ScenarioParser.apply(Scenario(), mapOf("pin_pad_seed" to 7))
        val fromLaunch = ScenarioParser.apply(Scenario(), mapOf("pin_pad_seed" to " 42 "))

        assertEquals(Scenario(pinPadSeed = 7), fromAdb.scenario)
        assertEquals(Scenario(pinPadSeed = 42), fromLaunch.scenario)
        assertTrue(fromAdb.rejected.isEmpty() && fromAdb.ignoredKeys.isEmpty())
        assertTrue(fromAdb.describe().contains("quote_ttl_s=30 pin_pad_seed=7"))
    }

    @Test
    fun `invalid pin pad seeds are rejected and keep the previous one, and defaults go back to the fixed order`() {
        val current = Scenario(pinPadSeed = 7)

        val update = ScenarioParser.apply(current, mapOf("pin_pad_seed" to "siete"))

        assertEquals(current, update.scenario)
        assertEquals(listOf("pin_pad_seed=siete"), update.rejected)
        assertEquals(listOf("pin_pad_seed=-1"), ScenarioParser.apply(current, mapOf("pin_pad_seed" to -1)).rejected)
        assertEquals(listOf("pin_pad_seed=1.5"), ScenarioParser.apply(current, mapOf("pin_pad_seed" to "1.5")).rejected)
        assertEquals(0, ScenarioParser.apply(current, mapOf("defaults" to true)).scenario.pinPadSeed)
        assertEquals(7, ScenarioParser.apply(current, mapOf("latency" to "0")).scenario.pinPadSeed)
        assertEquals(0, ScenarioParser.apply(current, mapOf("pin_pad_seed" to 0)).scenario.pinPadSeed)
    }

    @Test
    fun `the stored pin pad seed survives and feeds the client at once`() {
        val storage = FakeScenarioStorage()
        val store = ScenarioStore(storage)
        val source = ScenarioPinPadSeedSource(store)
        assertEquals(0, source.seed())

        store.update(mapOf("pin_pad_seed" to 7))

        assertEquals("7", storage.values["pin_pad_seed"])
        assertEquals(7, source.seed())
        assertEquals(7, ScenarioStore(storage).load().pinPadSeed)

        store.update(mapOf("defaults" to true))

        assertEquals(0, source.seed())
    }

    @Test
    fun `new endpoint ids take latency and failure rate per endpoint`() {
        val scenario = ScenarioParser.apply(
            Scenario(),
            mapOf(
                "latency" to "billers=2000,biller_debts=1500,bill_payments=900,bill_voucher=700",
                "failure_rate" to "bill_payments=100"
            )
        )

        assertTrue(scenario.rejected.isEmpty())
        assertEquals(2000L, scenario.scenario.latencyMs.valueFor("billers"))
        assertEquals(1500L, scenario.scenario.latencyMs.valueFor("biller_debts"))
        assertEquals(900L, scenario.scenario.latencyMs.valueFor("bill_payments"))
        assertEquals(700L, scenario.scenario.latencyMs.valueFor("bill_voucher"))
        assertEquals(100, scenario.scenario.failureRate.valueFor("bill_payments"))
        assertEquals(0, scenario.scenario.failureRate.valueFor("billers"))
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

    @Test
    fun `client flags are parsed from adb booleans and launch arguments text`() {
        val scenario = ScenarioParser.apply(
            Scenario(),
            mapOf(
                "movements_full_page" to true,
                "accounts_eager_refresh" to "TRUE",
                "breakdown_rich_transition" to "no"
            )
        )

        assertEquals(
            setOf(FeatureFlag.MOVEMENTS_FULL_PAGE, FeatureFlag.ACCOUNTS_EAGER_REFRESH),
            scenario.scenario.flags
        )
        assertEquals(listOf("breakdown_rich_transition=no"), scenario.rejected)
        assertTrue(scenario.ignoredKeys.isEmpty())
    }

    @Test
    fun `market flags are parsed from adb booleans and launch arguments text`() {
        val adb = ScenarioParser.apply(Scenario(), mapOf("quotes_full_refresh" to true, "chart_live_path" to true))
        val launchArguments = ScenarioParser.apply(
            Scenario(),
            mapOf("quotes_full_refresh" to "true", "chart_live_path" to "si")
        )

        assertEquals(setOf(FeatureFlag.QUOTES_FULL_REFRESH, FeatureFlag.CHART_LIVE_PATH), adb.scenario.flags)
        assertTrue(adb.rejected.isEmpty())
        assertTrue(adb.ignoredKeys.isEmpty())
        assertEquals(setOf(FeatureFlag.QUOTES_FULL_REFRESH), launchArguments.scenario.flags)
        assertEquals(listOf("chart_live_path=si"), launchArguments.rejected)
        assertTrue(adb.describe().contains("quotes_full_refresh=true chart_live_path=true"))
    }

    @Test
    fun `flags not sent keep their value and defaults turn them off`() {
        val current = Scenario(flags = setOf(FeatureFlag.BREAKDOWN_RICH_TRANSITION))

        assertEquals(current.flags, ScenarioParser.apply(current, mapOf("latency" to "0")).scenario.flags)
        assertEquals(emptySet<FeatureFlag>(), ScenarioParser.apply(current, mapOf("defaults" to true)).scenario.flags)
        assertEquals(
            emptySet<FeatureFlag>(),
            ScenarioParser.apply(current, mapOf("breakdown_rich_transition" to false)).scenario.flags
        )
    }

    @Test
    fun `stored flags survive and feed the client at once`() {
        val storage = FakeScenarioStorage()
        val store = ScenarioStore(storage)
        val source = ScenarioFeatureFlagSource(store)
        assertEquals(false, source.valueOf(FeatureFlag.MOVEMENTS_FULL_PAGE))

        store.update(mapOf("movements_full_page" to true))

        assertEquals("true", storage.values["movements_full_page"])
        assertEquals("false", storage.values["accounts_eager_refresh"])
        assertEquals(true, source.valueOf(FeatureFlag.MOVEMENTS_FULL_PAGE))
        assertEquals(setOf(FeatureFlag.MOVEMENTS_FULL_PAGE), ScenarioStore(storage).load().flags)
    }

    @Test
    fun `stored market flags survive and feed the client at once`() {
        val storage = FakeScenarioStorage()
        val store = ScenarioStore(storage)
        val source = ScenarioFeatureFlagSource(store)
        assertEquals(false, source.valueOf(FeatureFlag.QUOTES_FULL_REFRESH))
        assertEquals(false, source.valueOf(FeatureFlag.CHART_LIVE_PATH))

        store.update(mapOf("quotes_full_refresh" to true, "chart_live_path" to "true"))

        assertEquals("true", storage.values["quotes_full_refresh"])
        assertEquals("true", storage.values["chart_live_path"])
        assertEquals(true, source.valueOf(FeatureFlag.QUOTES_FULL_REFRESH))
        assertEquals(true, source.valueOf(FeatureFlag.CHART_LIVE_PATH))

        store.update(mapOf("chart_live_path" to false))

        assertEquals(setOf(FeatureFlag.QUOTES_FULL_REFRESH), ScenarioStore(storage).load().flags)
        assertEquals(false, source.valueOf(FeatureFlag.CHART_LIVE_PATH))
    }
}
