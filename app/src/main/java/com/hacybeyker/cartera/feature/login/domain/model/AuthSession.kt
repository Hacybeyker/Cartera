package com.hacybeyker.cartera.feature.login.domain.model

data class AuthSession(val accessToken: String, val expiresInSeconds: Long) {
    override fun toString(): String = "AuthSession(expiresInSeconds=$expiresInSeconds)"
}
