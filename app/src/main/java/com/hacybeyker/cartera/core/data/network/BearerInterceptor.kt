package com.hacybeyker.cartera.core.data.network

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class BearerInterceptor @Inject constructor(private val tokens: AccessTokenProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.accessToken() ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        return chain.proceed(request)
    }
}
