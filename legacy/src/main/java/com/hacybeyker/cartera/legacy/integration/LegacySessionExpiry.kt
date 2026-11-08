package com.hacybeyker.cartera.legacy.integration

fun interface LegacySessionExpiry {
    /** El servidor rechazó el token (401): quien aloja el módulo decide qué hacer con la sesión. */
    fun onSessionExpired()
}
