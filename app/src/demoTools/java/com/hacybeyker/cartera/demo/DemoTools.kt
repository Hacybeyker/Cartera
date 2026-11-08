package com.hacybeyker.cartera.demo

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import com.hacybeyker.cartera.demo.backend.DeviceRegistry
import com.hacybeyker.cartera.demo.scenario.ScenarioStorage
import com.hacybeyker.cartera.demo.scenario.ScenarioStore

const val DEMO_TAG = "CarteraDemo"
private const val SCENARIO_PREFERENCES = "cartera_demo_scenario"
private const val DEVICE_PREFERENCES = "cartera_demo_devices"

fun Context.scenarioStore(): ScenarioStore = ScenarioStore(
    SharedPreferencesScenarioStorage(
        applicationContext.getSharedPreferences(SCENARIO_PREFERENCES, Context.MODE_PRIVATE)
    )
)

fun Context.deviceRegistry(): DeviceRegistry = SharedPreferencesDeviceRegistry(
    applicationContext.getSharedPreferences(DEVICE_PREFERENCES, Context.MODE_PRIVATE)
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

/** Una clave por `deviceToken`, con el id de su usuario. Escribe con commit: el proceso puede morir enseguida. */
class SharedPreferencesDeviceRegistry(private val preferences: SharedPreferences) : DeviceRegistry {

    override fun userOf(deviceToken: String): String? = preferences.getString(deviceToken, null)

    override fun register(deviceToken: String, userId: String) {
        preferences.edit(commit = true) { putString(deviceToken, userId) }
    }

    override fun revokeAll(userId: String) {
        val revoked = preferences.all.filterValues { it == userId }.keys
        preferences.edit(commit = true) { revoked.forEach(::remove) }
    }

    override fun clear() {
        preferences.edit(commit = true) { clear() }
    }
}
