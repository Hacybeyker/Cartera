package com.hacybeyker.cartera.core.data.network

fun interface AccessTokenProvider {
    fun accessToken(): String?
}
