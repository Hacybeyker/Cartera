package com.hacybeyker.cartera.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.demo.scenario.DemoResetClient
import kotlin.concurrent.thread
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

// Exportado, pero protegido con android.permission.DUMP: solo adb puede enviarle algo.
class ScenarioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "${context.packageName}.demo.SET_SCENARIO" -> {
                val update = context.scenarioStore().update(intent.extras.toScenarioValues())
                Log.i(DEMO_TAG, update.describe())
            }
            "${context.packageName}.demo.RESET_DATA" -> resetData()
            else -> Log.w(DEMO_TAG, "Acción desconocida: ${intent.action}")
        }
    }

    private fun resetData() {
        val pending = goAsync()
        thread(name = "CarteraDemo-reset") {
            try {
                val reset = DemoResetClient(OkHttpClient(), BuildConfig.BASE_URL.toHttpUrl()).reset()
                Log.i(DEMO_TAG, if (reset) "Datos restablecidos a los fixtures" else "No se pudo restablecer los datos")
            } finally {
                pending.finish()
            }
        }
    }
}
