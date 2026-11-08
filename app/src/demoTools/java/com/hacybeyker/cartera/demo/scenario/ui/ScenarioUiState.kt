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
    val pinPadSeed: String = "",
    val flags: Set<FeatureFlag> = emptySet(),
    val isResettingData: Boolean = false,
    val message: ScenarioMessage? = null
) {
    fun edited(action: ScenarioAction.FieldEdit): ScenarioUiState = when (action) {
        is ScenarioAction.LatencyChanged -> copy(latency = action.value, message = null)
        is ScenarioAction.FailureRateChanged -> copy(failureRate = action.value, message = null)
        is ScenarioAction.FailureModeSelected -> copy(failureMode = action.mode, message = null)
        is ScenarioAction.SessionTtlChanged -> copy(sessionTtl = action.value, message = null)
        is ScenarioAction.BackendDownChanged -> copy(backendDown = action.value, message = null)
        is ScenarioAction.TickMsChanged -> copy(tickMs = action.value, message = null)
        is ScenarioAction.StreamDropChanged -> copy(streamDrop = action.value, message = null)
        is ScenarioAction.QuoteTtlChanged -> copy(quoteTtl = action.value, message = null)
        is ScenarioAction.PinPadSeedChanged -> copy(pinPadSeed = action.value, message = null)
        is ScenarioAction.FlagChanged -> {
            val flags = if (action.enabled) flags + action.flag else flags - action.flag
            copy(flags = flags, message = null)
        }
    }

    fun withScenario(scenario: Scenario): ScenarioUiState = copy(
        latency = scenario.latencyMs.encode(),
        failureRate = scenario.failureRate.encode(),
        failureMode = scenario.failureMode,
        sessionTtl = scenario.sessionTtlSeconds.toString(),
        backendDown = scenario.backendDown,
        tickMs = scenario.tickMs.toString(),
        streamDrop = scenario.streamDropSeconds.toString(),
        quoteTtl = scenario.quoteTtlSeconds.toString(),
        pinPadSeed = scenario.pinPadSeed.toString(),
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
    /** Cambia un campo del formulario; no guarda nada hasta Guardar. */
    sealed interface FieldEdit : ScenarioAction

    data class LatencyChanged(val value: String) : FieldEdit

    data class FailureRateChanged(val value: String) : FieldEdit

    data class FailureModeSelected(val mode: FailureMode) : FieldEdit

    data class SessionTtlChanged(val value: String) : FieldEdit

    data class BackendDownChanged(val value: Boolean) : FieldEdit

    data class TickMsChanged(val value: String) : FieldEdit

    data class StreamDropChanged(val value: String) : FieldEdit

    data class QuoteTtlChanged(val value: String) : FieldEdit

    data class PinPadSeedChanged(val value: String) : FieldEdit

    data class FlagChanged(val flag: FeatureFlag, val enabled: Boolean) : FieldEdit

    data object SaveClicked : ScenarioAction

    data object DefaultsClicked : ScenarioAction

    data object ResetDataClicked : ScenarioAction
}
