package com.hacybeyker.cartera.demo.scenario

import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DemoResetClient(private val client: OkHttpClient, private val baseUrl: HttpUrl) {

    fun reset(): Boolean {
        val request = Request.Builder()
            .url(baseUrl.resolve("_demo/reset") ?: return false)
            .post(ByteArray(0).toRequestBody())
            .build()
        return try {
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: IOException) {
            false
        }
    }
}
