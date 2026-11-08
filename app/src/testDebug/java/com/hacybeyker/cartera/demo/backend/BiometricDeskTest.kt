package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricDeskTest {
    private val harness = RouterHarness()
    private val token = harness.login()

    @Test
    fun `enroll returns an opaque device token`() {
        val response = harness.call("POST", "auth/biometric/enroll", token = token)
        val deviceToken = response.json().string("deviceToken")

        assertEquals(201, response.status)
        assertTrue(deviceToken.matches(Regex("dvc_[A-Za-z0-9_-]{43}")))
        assertNotEquals(deviceToken, enroll())
    }

    @Test
    fun `login with the device token answers like the password login`() {
        val response = biometricLogin(enroll())
        val body = response.json()

        assertEquals(200, response.status)
        assertEquals("Bearer", body.string("tokenType"))
        assertEquals(900L, body.getValue("expiresIn").jsonPrimitive.long)
        assertEquals(TokenCheck.Valid("USR-001"), harness.jwt.check(body.string("accessToken")))
    }

    @Test
    fun `login honors the session ttl header`() {
        val response = biometricLogin(enroll(), headers = mapOf("X-Demo-Session-Ttl" to "60"))

        assertEquals(60L, response.json().getValue("expiresIn").jsonPrimitive.long)
    }

    @Test
    fun `unknown device token is an invalid device`() {
        val response = biometricLogin("dvc_unknown")

        assertEquals(401, response.status)
        assertEquals("INVALID_DEVICE", response.problemCode())
    }

    @Test
    fun `missing device token is a validation error`() {
        val response = harness.call("POST", "auth/biometric/login", body = "{}")

        assertEquals(listOf("deviceToken"), response.errorFields())
    }

    @Test
    fun `delete revokes every device of the user`() {
        val first = enroll()
        val second = enroll()

        val response = harness.call("DELETE", "auth/biometric", token = token)

        assertEquals(204, response.status)
        assertEquals("INVALID_DEVICE", biometricLogin(first).problemCode())
        assertEquals("INVALID_DEVICE", biometricLogin(second).problemCode())
    }

    @Test
    fun `enroll and delete need a session`() {
        assertEquals("UNAUTHORIZED", harness.call("POST", "auth/biometric/enroll").problemCode())
        assertEquals("UNAUTHORIZED", harness.call("DELETE", "auth/biometric").problemCode())
    }

    @Test
    fun `reset forgets registered devices`() {
        val deviceToken = enroll()

        harness.call("POST", "_demo/reset")

        assertEquals("INVALID_DEVICE", biometricLogin(deviceToken).problemCode())
    }

    @Test
    fun `device token of a user that no longer exists is invalid`() {
        harness.state.deviceTokens["dvc_orphan"] = "USR-404"

        assertEquals("INVALID_DEVICE", biometricLogin("dvc_orphan").problemCode())
    }

    @Test
    fun `token generator can be replaced`() {
        val desk = BiometricDesk(harness.state, harness.desks.sessions) { "dvc_fixed" }

        assertEquals("""{"deviceToken":"dvc_fixed"}""", desk.enroll("USR-001").body)
        assertEquals("USR-001", harness.state.deviceTokens["dvc_fixed"])
    }

    private fun enroll(): String =
        harness.call("POST", "auth/biometric/enroll", token = token).json().string("deviceToken")

    private fun biometricLogin(deviceToken: String, headers: Map<String, String> = emptyMap()) = harness.call(
        method = "POST",
        path = "auth/biometric/login",
        body = """{"deviceToken":"$deviceToken"}""",
        headers = headers
    )
}
