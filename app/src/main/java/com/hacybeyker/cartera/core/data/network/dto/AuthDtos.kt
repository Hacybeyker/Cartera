package com.hacybeyker.cartera.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppConfigDto(val minSupportedVersion: String, val termsVersion: String)

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class LoginResponseDto(val accessToken: String, val tokenType: String, val expiresIn: Long)

@Serializable
data class MeDto(val id: String, val fullName: String, val documentMasked: String, val email: String)
