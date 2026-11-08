package com.hacybeyker.cartera.legacy.integration

import okhttp3.Call

fun interface LegacyHttpClient {
    /** El cliente HTTP de quien aloja el módulo, con su sesión y sus interceptores ya puestos. */
    fun callFactory(): Call.Factory
}
