package com.hacybeyker.cartera.demo.backend

import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Sesión del token Bearer de una llamada: el canal en vivo la cierra con 4401 al llegar a [expiresAt]. */
data class Session(val userId: String, val expiresAt: Instant)

class SessionDesk(private val state: BackendState, private val jwt: JwtSigner) {

    fun login(request: BackendRequest): BackendResponse {
        val body = request.jsonBody()
        val username = (body["username"] as? JsonPrimitive)?.content
        val password = (body["password"] as? JsonPrimitive)?.content
        val missing = listOfNotNull(
            "username".takeIf { username.isNullOrBlank() },
            "password".takeIf { password.isNullOrBlank() }
        )
        if (missing.isNotEmpty()) {
            throw ProblemException(
                code = ProblemCode.VALIDATION_ERROR,
                detail = "Faltan datos para iniciar sesión.",
                errors = missing.map { FieldError(it, "REQUIRED", "El campo es obligatorio.") }
            )
        }
        val user = state.userByUsername(username.orEmpty())?.takeIf { it.string("password") == password }
            ?: throw ProblemException(ProblemCode.INVALID_CREDENTIALS, "Usuario o contraseña incorrectos.")
        return issue(user.string("id"), request)
    }

    /** La respuesta de un login correcto; `X-Demo-Session-Ttl` fija la vida del token. */
    fun issue(userId: String, request: BackendRequest): BackendResponse {
        val ttl = request.header(DemoScenario.SESSION_TTL_HEADER)?.toLongOrNull()?.takeIf { it > 0 }
            ?: DEFAULT_SESSION_TTL
        return json(
            OK,
            buildJsonObject {
                put("accessToken", jwt.issue(userId, ttl))
                put("tokenType", "Bearer")
                put("expiresIn", ttl)
            }
        )
    }

    fun authenticate(request: BackendRequest): String = session(request).userId

    fun session(request: BackendRequest): Session {
        val token = request.header("Authorization")?.removePrefix("Bearer ")?.trim().orEmpty()
        return when (val check = jwt.check(token)) {
            is TokenCheck.Valid -> Session(check.subject, check.expiresAt).takeIf { state.user(it.userId) != null }
            TokenCheck.Expired ->
                throw ProblemException(ProblemCode.SESSION_EXPIRED, "La sesión expiró. Vuelve a ingresar.")
            TokenCheck.Invalid -> null
        } ?: throw ProblemException(ProblemCode.UNAUTHORIZED, "Falta un token válido.")
    }

    private companion object {
        const val OK = 200
        const val DEFAULT_SESSION_TTL = 900L
    }
}
