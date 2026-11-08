package com.hacybeyker.cartera.feature.legacy.data

import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import javax.inject.Inject
import okhttp3.Call
import okhttp3.OkHttpClient

/**
 * Entrega a `:legacy` el mismo cliente HTTP de la app: así sus llamadas llevan el token y pasan por los interceptores
 * del escenario (latencia y fallos por endpoint) igual que las de Compose.
 */
class OkHttpLegacyHttpClient @Inject constructor(private val client: OkHttpClient) : LegacyHttpClient {
    override fun callFactory(): Call.Factory = client
}
