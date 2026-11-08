package com.hacybeyker.cartera.core.testing

import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.data.network.MarketApi
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import retrofit2.Retrofit

fun MockWebServer.carteraApi(): CarteraApi = retrofit().create(CarteraApi::class.java)

fun MockWebServer.marketApi(): MarketApi = retrofit().create(MarketApi::class.java)

private fun MockWebServer.retrofit(): Retrofit {
    val client = CarteraHttpClient.okHttpClient(timeoutSeconds = 1, interceptors = emptyList())
    return CarteraHttpClient.retrofit(url("/api/v1/").toString(), client)
}

fun MockWebServer.enqueueJson(code: Int, body: String, contentType: String = "application/json") {
    enqueue(MockResponse.Builder().code(code).setHeader("Content-Type", contentType).body(body).build())
}

fun problemJson(code: String, status: Int): String =
    """{"type":"about:blank","title":"t","status":$status,"detail":"detalle","code":"$code","traceId":"req-1"}"""
