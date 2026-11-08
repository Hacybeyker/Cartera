package com.hacybeyker.cartera.legacy.integration

/** La sesión de quien aloja el módulo: el módulo no la inicia ni la cierra, solo la usa. */
fun interface LegacySession {
    /** El token de acceso vigente, o `null` si no hay sesión. */
    fun accessToken(): String?
}
