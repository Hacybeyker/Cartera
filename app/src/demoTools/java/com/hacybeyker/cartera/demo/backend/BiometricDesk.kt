package com.hacybeyker.cartera.demo.backend

import java.security.SecureRandom
import java.util.Base64
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Ingreso con huella: el dispositivo se registra una vez y luego cambia su `deviceToken` por una sesión. */
class BiometricDesk(
    private val state: BackendState,
    private val sessions: SessionDesk,
    private val newToken: () -> String = ::randomDeviceToken
) {

    fun enroll(userId: String): BackendResponse {
        val token = newToken()
        state.deviceTokens[token] = userId
        return json(CREATED, buildJsonObject { put("deviceToken", token) })
    }

    fun login(request: BackendRequest): BackendResponse {
        val token = request.jsonBody().text("deviceToken")
        if (token.isNullOrBlank()) {
            throw ProblemException(
                code = ProblemCode.VALIDATION_ERROR,
                detail = "Falta el token del dispositivo.",
                errors = listOf(FieldError("deviceToken", "REQUIRED", "El campo es obligatorio."))
            )
        }
        val userId = state.deviceTokens[token]?.takeIf { state.user(it) != null }
            ?: throw ProblemException(
                ProblemCode.INVALID_DEVICE,
                "El dispositivo no está registrado. Ingresa con tu clave."
            )
        return sessions.issue(userId, request)
    }

    fun revoke(userId: String): BackendResponse {
        state.deviceTokens.values.removeAll { it == userId }
        return BackendResponse(status = NO_CONTENT)
    }

    private companion object {
        const val CREATED = 201
        const val NO_CONTENT = 204
    }
}

private const val DEVICE_TOKEN_BYTES = 32
private val random = SecureRandom()

fun randomDeviceToken(): String {
    val bytes = ByteArray(DEVICE_TOKEN_BYTES).also(random::nextBytes)
    return "dvc_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
