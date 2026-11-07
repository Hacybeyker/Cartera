package com.hacybeyker.cartera.demo.backend

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Test

class JwtSignerTest {
    private val clock = MutableClock()
    private val signer = JwtSigner(clock)
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    @Test
    fun `issued token is valid until its ttl`() {
        val token = signer.issue("USR-001", ttlSeconds = 60)

        assertEquals(TokenCheck.Valid("USR-001"), signer.check(token))
        clock.now = clock.now.plusSeconds(59)
        assertEquals(TokenCheck.Valid("USR-001"), signer.check(token))
    }

    @Test
    fun `token is expired once its ttl passes`() {
        val token = signer.issue("USR-001", ttlSeconds = 60)

        clock.now = clock.now.plusSeconds(60)

        assertEquals(TokenCheck.Expired, signer.check(token))
    }

    @Test
    fun `token signed with another key is invalid`() {
        val foreign = JwtSigner(clock, key = "otra-clave").issue("USR-001", ttlSeconds = 60)

        assertEquals(TokenCheck.Invalid, signer.check(foreign))
    }

    @Test
    fun `tampered payload is invalid`() {
        val (header, _, signature) = signer.issue("USR-001", ttlSeconds = 60).split('.')
        val payload = encoder.encodeToString("""{"sub":"USR-002","iat":0,"exp":9999999999}""".toByteArray())

        assertEquals(TokenCheck.Invalid, signer.check("$header.$payload.$signature"))
    }

    @Test
    fun `malformed tokens are invalid`() {
        assertEquals(TokenCheck.Invalid, signer.check(""))
        assertEquals(TokenCheck.Invalid, signer.check("a.b"))
        assertEquals(TokenCheck.Invalid, signer.check(signed("no-es-json")))
        assertEquals(TokenCheck.Invalid, signer.check(signed("""{"sub":"USR-001"}""")))
        assertEquals(TokenCheck.Invalid, signer.check(signed("""{"sub":"USR-001","exp":"pronto"}""")))
        assertEquals(TokenCheck.Invalid, signer.check(signedRaw("%%%")))
    }

    private fun signed(payload: String) = signedRaw(encoder.encodeToString(payload.toByteArray()))

    private fun signedRaw(encodedPayload: String): String {
        val header = signer.issue("x", 1).substringBefore('.')
        val unsigned = "$header.$encodedPayload"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(JwtSigner.DEMO_KEY.toByteArray(), "HmacSHA256"))
        return "$unsigned.${encoder.encodeToString(mac.doFinal(unsigned.toByteArray()))}"
    }
}
