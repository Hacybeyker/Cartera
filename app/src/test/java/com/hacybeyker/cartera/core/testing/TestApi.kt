package com.hacybeyker.cartera.core.testing

import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer

fun MockWebServer.carteraApi(): CarteraApi {
    val client = CarteraHttpClient.okHttpClient(timeoutSeconds = 1, interceptors = emptyList())
    return CarteraHttpClient.retrofit(url("/api/v1/").toString(), client).create(CarteraApi::class.java)
}

fun MockWebServer.enqueueJson(code: Int, body: String, contentType: String = "application/json") {
    enqueue(MockResponse.Builder().code(code).setHeader("Content-Type", contentType).body(body).build())
}

fun problemJson(code: String, status: Int): String =
    """{"type":"about:blank","title":"t","status":$status,"detail":"detalle","code":"$code","traceId":"req-1"}"""
