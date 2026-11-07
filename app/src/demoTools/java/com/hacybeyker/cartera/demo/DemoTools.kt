package com.hacybeyker.cartera.demo

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import com.hacybeyker.cartera.demo.scenario.ScenarioStorage
import com.hacybeyker.cartera.demo.scenario.ScenarioStore

const val DEMO_TAG = "CarteraDemo"
private const val SCENARIO_PREFERENCES = "cartera_demo_scenario"

fun Context.scenarioStore(): ScenarioStore = ScenarioStore(
    SharedPreferencesScenarioStorage(
        applicationContext.getSharedPreferences(SCENARIO_PREFERENCES, Context.MODE_PRIVATE)
    )
)

// Los extras de adb y de launchApp llegan con su tipo (texto, entero, booleano); el parser acepta cualquiera.
@Suppress("DEPRECATION")
fun Bundle?.toScenarioValues(): Map<String, Any?> = this?.keySet()?.associateWith { get(it) }.orEmpty()

class SharedPreferencesScenarioStorage(private val preferences: SharedPreferences) : ScenarioStorage {

    override fun read(): Map<String, String> =
        preferences.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()

    override fun write(values: Map<String, String>) {
        preferences.edit {
            clear()
            values.forEach { (key, value) -> putString(key, value) }
        }
    }
}
