package com.hacybeyker.cartera.demo.scenario.ui

import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.demo.backend.FailureMode
import com.hacybeyker.cartera.demo.scenario.ScenarioKeys

/** Cada campo se llama `escenario_<clave de adb>`, igual que su extra. */
object ScenarioTestTags {
    const val SCREEN = "escenario_pantalla"
    const val LATENCY = "escenario_${ScenarioKeys.LATENCY}"
    const val FAILURE_RATE = "escenario_${ScenarioKeys.FAILURE_RATE}"
    const val FAILURE_MODE = "escenario_${ScenarioKeys.FAILURE_MODE}"
    const val SESSION_TTL = "escenario_${ScenarioKeys.SESSION_TTL}"
    const val BACKEND_DOWN = "escenario_${ScenarioKeys.BACKEND_DOWN}"
    const val TICK_MS = "escenario_${ScenarioKeys.TICK_MS}"
    const val STREAM_DROP = "escenario_${ScenarioKeys.STREAM_DROP}"
    const val QUOTE_TTL = "escenario_${ScenarioKeys.QUOTE_TTL}"
    const val SAVE = "escenario_guardar"
    const val DEFAULTS = "escenario_valores_defecto"
    const val RESET_DATA = "escenario_restablecer_datos"
    const val MESSAGE = "escenario_mensaje"

    fun failureMode(mode: FailureMode) = "${FAILURE_MODE}_${mode.wireName}"

    fun flag(flag: FeatureFlag) = "escenario_${flag.key}"
}
