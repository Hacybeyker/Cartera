package com.hacybeyker.cartera.core.data.network

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object CarteraHttpClient {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // Sin reintentos automáticos: cada fallo de red inyectado debe llegar a la UI.
    fun okHttpClient(timeoutSeconds: Long, interceptors: Iterable<Interceptor>): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .apply { interceptors.forEach(::addInterceptor) }
        .build()

    fun retrofit(baseUrl: String, client: OkHttpClient, json: Json = this.json): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
