package com.hacybeyker.cartera.feature.login.domain.model

data class Credentials(val username: String, val password: String) {
    // Sin la contraseña: evita que termine en un log o en un reporte de error.
    override fun toString(): String = "Credentials(username=$username)"
}
