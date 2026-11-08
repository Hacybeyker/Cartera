package com.hacybeyker.cartera.demo.scenario.ui

import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.demo.backend.FailureMode
import com.hacybeyker.cartera.demo.scenario.Scenario

/** Los campos son texto, con el mismo formato que los extras de adb, para que se puedan copiar de un lado al otro. */
data class ScenarioUiState(
    val latency: String = "",
    val failureRate: String = "",
    val failureMode: FailureMode = FailureMode.HTTP_500,
    val sessionTtl: String = "",
    val backendDown: Boolean = false,
    val tickMs: String = "",
    val streamDrop: String = "",
    val quoteTtl: String = "",
    val flags: Set<FeatureFlag> = emptySet(),
    val isResettingData: Boolean = false,
    val message: ScenarioMessage? = null
) {
    fun withScenario(scenario: Scenario): ScenarioUiState = copy(
        latency = scenario.latencyMs.encode(),
        failureRate = scenario.failureRate.encode(),
        failureMode = scenario.failureMode,
        sessionTtl = scenario.sessionTtlSeconds.toString(),
        backendDown = scenario.backendDown,
        tickMs = scenario.tickMs.toString(),
        streamDrop = scenario.streamDropSeconds.toString(),
        quoteTtl = scenario.quoteTtlSeconds.toString(),
        flags = scenario.flags
    )
}

sealed interface ScenarioMessage {
    data object Saved : ScenarioMessage

    data class Rejected(val values: List<String>) : ScenarioMessage

    data object DefaultsRestored : ScenarioMessage

    data object DataReset : ScenarioMessage

    data object DataResetFailed : ScenarioMessage
}

sealed interface ScenarioAction {
    data class LatencyChanged(val value: String) : ScenarioAction

    data class FailureRateChanged(val value: String) : ScenarioAction

    data class FailureModeSelected(val mode: FailureMode) : ScenarioAction

    data class SessionTtlChanged(val value: String) : ScenarioAction

    data class BackendDownChanged(val value: Boolean) : ScenarioAction

    data class TickMsChanged(val value: String) : ScenarioAction

    data class StreamDropChanged(val value: String) : ScenarioAction

    data class QuoteTtlChanged(val value: String) : ScenarioAction

    data class FlagChanged(val flag: FeatureFlag, val enabled: Boolean) : ScenarioAction

    data object SaveClicked : ScenarioAction

    data object DefaultsClicked : ScenarioAction

    data object ResetDataClicked : ScenarioAction
}
