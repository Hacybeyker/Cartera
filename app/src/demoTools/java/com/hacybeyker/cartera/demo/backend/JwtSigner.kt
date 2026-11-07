package com.hacybeyker.cartera.demo.backend

import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

sealed interface TokenCheck {
    data class Valid(val subject: String) : TokenCheck
    data object Expired : TokenCheck
    data object Invalid : TokenCheck
}

class JwtSigner(private val clock: Clock, key: String = DEMO_KEY) {
    private val keySpec = SecretKeySpec(key.toByteArray(), ALGORITHM)
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun issue(subject: String, ttlSeconds: Long): String {
        val issuedAt = clock.instant().epochSecond
        val payload = buildJsonObject {
            put("sub", subject)
            put("iat", issuedAt)
            put("exp", issuedAt + ttlSeconds)
        }
        val unsigned = "${encode(HEADER)}.${encode(payload.toString())}"
        return "$unsigned.${sign(unsigned)}"
    }

    fun check(token: String): TokenCheck {
        val parts = token.split('.')
        val claims = if (parts.size == PARTS && signatureMatches(parts)) decodeClaims(parts[1]) else null
        return when {
            claims == null -> TokenCheck.Invalid
            clock.instant().epochSecond >= claims.expiresAt -> TokenCheck.Expired
            else -> TokenCheck.Valid(claims.subject)
        }
    }

    // SerializationException hereda de IllegalArgumentException: un catch cubre Base64, JSON y tipos.
    private fun decodeClaims(encoded: String): Claims? = try {
        val claims = Json.parseToJsonElement(String(decoder.decode(encoded))).jsonObject
        val subject = claims["sub"]?.jsonPrimitive?.contentOrNull
        val expiresAt = claims["exp"]?.jsonPrimitive?.longOrNull
        if (subject != null && expiresAt != null) Claims(subject, expiresAt) else null
    } catch (_: IllegalArgumentException) {
        null
    }

    private data class Claims(val subject: String, val expiresAt: Long)

    private fun signatureMatches(parts: List<String>): Boolean {
        val expected = sign("${parts[0]}.${parts[1]}").toByteArray()
        return MessageDigest.isEqual(expected, parts[2].toByteArray())
    }

    private fun sign(input: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(keySpec)
        return encoder.encodeToString(mac.doFinal(input.toByteArray()))
    }

    private fun encode(value: String): String = encoder.encodeToString(value.toByteArray())

    companion object {
        // Clave de demo, publicada a propósito en api/openapi.yaml: no protege nada real.
        const val DEMO_KEY = "cartera-demo-hs256-key-2026-not-a-secret"
        private const val ALGORITHM = "HmacSHA256"
        private const val HEADER = """{"alg":"HS256","typ":"JWT"}"""
        private const val PARTS = 3
    }
}
