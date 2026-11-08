package com.hacybeyker.cartera.demo

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.hacybeyker.cartera.demo.backend.CarteraBackend
import java.io.IOException
import kotlin.concurrent.thread

// Un ContentProvider se crea antes que la Application: el backend escucha antes de la primera llamada y los
// argumentos de arranque se aplican antes de que la primera pantalla pida algo.
class DemoToolsProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = requireNotNull(context)
        val backend = CarteraBackend(
            fixtures = { path -> context.assets.open(path).bufferedReader().use { it.readText() } },
            devices = context.deviceRegistry(),
            log = { message -> Log.i(BACKEND_TAG, message) }
        )
        // El socket se abre fuera del hilo principal, pero se espera: la splash llama apenas se abre la app.
        thread(name = "CarteraBackend-start") {
            try {
                backend.start()
                Log.i(BACKEND_TAG, "Escuchando en ${backend.baseUrl}")
            } catch (exception: IOException) {
                Log.e(BACKEND_TAG, "No se pudo abrir el puerto ${CarteraBackend.PORT}", exception)
            }
        }.join()
        (context.applicationContext as Application)
            .registerActivityLifecycleCallbacks(LaunchArgumentsCallbacks(context.scenarioStore()))
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0

    private companion object {
        const val BACKEND_TAG = "CarteraBackend"
    }
}
