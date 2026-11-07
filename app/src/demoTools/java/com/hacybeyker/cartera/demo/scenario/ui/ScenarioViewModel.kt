package com.hacybeyker.cartera.demo.scenario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.common.IoDispatcher
import com.hacybeyker.cartera.demo.scenario.DemoResetClient
import com.hacybeyker.cartera.demo.scenario.ScenarioKeys
import com.hacybeyker.cartera.demo.scenario.ScenarioStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Edita el mismo escenario que el receptor de adb y los argumentos de arranque, con las mismas reglas. */
@HiltViewModel
class ScenarioViewModel @Inject constructor(
    private val store: ScenarioStore,
    private val resetClient: DemoResetClient,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    private val state = MutableStateFlow(ScenarioUiState())
    val uiState: StateFlow<ScenarioUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val scenario = withContext(io) { store.load() }
            state.update { it.withScenario(scenario) }
        }
    }

    fun onAction(action: ScenarioAction) {
        when (action) {
            is ScenarioAction.LatencyChanged -> state.update { it.copy(latency = action.value, message = null) }
            is ScenarioAction.FailureRateChanged -> state.update { it.copy(failureRate = action.value, message = null) }
            is ScenarioAction.FailureModeSelected -> state.update { it.copy(failureMode = action.mode, message = null) }
            is ScenarioAction.SessionTtlChanged -> state.update { it.copy(sessionTtl = action.value, message = null) }
            is ScenarioAction.BackendDownChanged -> state.update { it.copy(backendDown = action.value, message = null) }
            ScenarioAction.SaveClicked -> save()
            ScenarioAction.DefaultsClicked -> restoreDefaults()
            ScenarioAction.ResetDataClicked -> resetData()
        }
    }

    private fun save() {
        val form = state.value
        val values = mapOf(
            ScenarioKeys.LATENCY to form.latency,
            ScenarioKeys.FAILURE_RATE to form.failureRate,
            ScenarioKeys.FAILURE_MODE to form.failureMode.wireName,
            ScenarioKeys.SESSION_TTL to form.sessionTtl,
            ScenarioKeys.BACKEND_DOWN to form.backendDown
        )
        viewModelScope.launch {
            val update = withContext(io) { store.update(values) }
            val message = when {
                update.rejected.isEmpty() -> ScenarioMessage.Saved
                else -> ScenarioMessage.Rejected(update.rejected)
            }
            state.update { it.withScenario(update.scenario).copy(message = message) }
        }
    }

    private fun restoreDefaults() {
        viewModelScope.launch {
            val update = withContext(io) { store.update(mapOf(ScenarioKeys.DEFAULTS to true)) }
            state.update { it.withScenario(update.scenario).copy(message = ScenarioMessage.DefaultsRestored) }
        }
    }

    private fun resetData() {
        if (state.value.isResettingData) return
        state.update { it.copy(isResettingData = true, message = null) }
        viewModelScope.launch {
            val reset = withContext(io) { resetClient.reset() }
            val message = if (reset) ScenarioMessage.DataReset else ScenarioMessage.DataResetFailed
            state.update { it.copy(isResettingData = false, message = message) }
        }
    }
}
