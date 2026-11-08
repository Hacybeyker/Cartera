package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendRouterTest {
    private val harness = RouterHarness()

    @Test
    fun `app config is public and carries the terms version`() {
        val response = harness.get("app/config")

        assertEquals(200, response.status)
        assertEquals("2026-11-01", response.json().string("termsVersion"))
        assertEquals("0.1.0", response.json().string("minSupportedVersion"))
    }

    @Test
    fun `login with the test user returns a bearer token with the default ttl`() {
        val response = harness.call(
            "POST",
            "auth/login",
            body = """{"username":"ana.torres","password":"Cartera2026"}"""
        )

        assertEquals(200, response.status)
        assertEquals("Bearer", response.json().string("tokenType"))
        assertEquals(900L, response.json().getValue("expiresIn").jsonPrimitive.long)
        assertEquals(TokenCheck.Valid("USR-001"), harness.jwt.check(response.json().string("accessToken")))
    }

    @Test
    fun `login honors the session ttl header`() {
        val response = harness.call(
            method = "POST",
            path = "auth/login",
            body = """{"username":"ana.torres","password":"Cartera2026"}""",
            headers = mapOf("X-Demo-Session-Ttl" to "60")
        )

        assertEquals(60L, response.json().getValue("expiresIn").jsonPrimitive.long)
    }

    @Test
    fun `login with a wrong password is invalid credentials`() {
        val response = harness.call("POST", "auth/login", body = """{"username":"ana.torres","password":"x"}""")

        assertProblem(response, 401, "INVALID_CREDENTIALS")
    }

    @Test
    fun `login without fields or with a broken body is a validation error`() {
        val empty = harness.call("POST", "auth/login", body = "")
        val broken = harness.call("POST", "auth/login", body = "{nope")

        assertProblem(empty, 422, "VALIDATION_ERROR")
        assertEquals(
            listOf("username", "password"),
            empty.json().getValue("errors").jsonArray.map { it.jsonObject.string("field") }
        )
        assertProblem(broken, 422, "VALIDATION_ERROR")
    }

    @Test
    fun `problems follow rfc 9457`() {
        val response = harness.get("me")
        val body = response.json()

        assertEquals(BackendResponse.PROBLEM_JSON, response.contentType)
        assertEquals("https://cartera.invalid/problems/unauthorized", body.string("type"))
        assertEquals("No autorizado", body.string("title"))
        assertEquals(401L, body.getValue("status").jsonPrimitive.long)
        assertEquals("req-test", body.string("traceId"))
        assertTrue(body.string("detail").isNotBlank())
        assertFalse(body.containsKey("errors"))
    }

    @Test
    fun `authenticated endpoints reject missing, invalid and orphan tokens`() {
        assertProblem(harness.get("me"), 401, "UNAUTHORIZED")
        assertProblem(harness.get("me", token = "basura"), 401, "UNAUTHORIZED")
        assertProblem(harness.get("me", token = harness.jwt.issue("USR-999", 60)), 401, "UNAUTHORIZED")
    }

    @Test
    fun `expired token is session expired`() {
        val token = harness.login()

        harness.clock.now = harness.clock.now.plusSeconds(900)

        assertProblem(harness.get("accounts", token), 401, "SESSION_EXPIRED")
    }

    @Test
    fun `me returns only the public profile`() {
        val body = harness.get("me", harness.login()).json()

        assertEquals(setOf("id", "fullName", "documentMasked", "email"), body.keys)
        assertEquals("Ana Torres", body.string("fullName"))
    }

    @Test
    fun `accounts list the reference accounts without the owner`() {
        val items = harness.get("accounts", harness.login()).json().items().map { it.jsonObject }

        assertEquals(listOf("ACC-001", "ACC-002", "ACC-003"), items.map { it.string("id") })
        assertEquals(listOf("8450.75", "2300.00", "1050.20"), items.map { it.obj("availableBalance").string("value") })
        assertTrue(items.none { it.containsKey("ownerId") })
    }

    @Test
    fun `account by id returns it or not found`() {
        val token = harness.login()

        assertEquals("Cuenta Dólares", harness.get("accounts/ACC-003", token).json().string("alias"))
        assertProblem(harness.get("accounts/ACC-404", token), 404, "NOT_FOUND")
    }

    @Test
    fun `movements default to twenty of every account, newest first`() {
        val body = harness.get("movements", harness.login()).json()
        val items = body.items().map { it.jsonObject }

        assertEquals(20, items.size)
        assertEquals("MOV-001-0001", items.first().string("id"))
        assertEquals(items.sortedByDescending { it.string("date") }, items)
        assertEquals(
            setOf("id", "accountId", "date", "description", "category", "amount", "type"),
            items.first().keys
        )
        assertTrue(body.getValue("nextCursor") != JsonNull)
    }

    @Test
    fun `account movements page through all 1200 with the cursor`() {
        val token = harness.login()
        val first = harness.get("movements", token, mapOf("accountId" to "ACC-001", "limit" to "1000")).json()
        val cursor = first.string("nextCursor")
        val second = harness.get(
            "movements",
            token,
            mapOf(
                "accountId" to "ACC-001",
                "limit" to "1000",
                "cursor" to cursor
            )
        )
            .json()

        assertEquals(1000, first.items().size)
        assertEquals(200, second.items().size)
        assertEquals(JsonNull, second.getValue("nextCursor"))
    }

    @Test
    fun `movements reject a foreign account, a bad limit and a bad cursor`() {
        val token = harness.login()

        assertProblem(harness.get("movements", token, mapOf("accountId" to "ACC-404")), 422, "VALIDATION_ERROR")
        assertProblem(harness.get("movements", token, mapOf("limit" to "0")), 422, "VALIDATION_ERROR")
        assertProblem(harness.get("movements", token, mapOf("limit" to "1001")), 422, "VALIDATION_ERROR")
        assertProblem(harness.get("movements", token, mapOf("limit" to "veinte")), 422, "VALIDATION_ERROR")
        assertProblem(harness.get("movements", token, mapOf("cursor" to "%%%")), 422, "VALIDATION_ERROR")
    }

    @Test
    fun `movement detail adds the operation data`() {
        val body = harness.get("movements/MOV-002-0001", harness.login()).json()

        assertEquals("20000001", body.string("operationNumber"))
        assertEquals("POS", body.string("channel"))
        assertEquals("COMPLETED", body.string("status"))
        assertEquals("Supermercado La Canasta", body.obj("counterparty").string("name"))
        assertFalse(body.containsKey("breakdown"))
    }

    @Test
    fun `breakdown of the big grocery has forty lines`() {
        val body = harness.get("movements/MOV-002-0001/breakdown", harness.login()).json()

        assertEquals(40, body.getValue("lines").jsonArray.size)
        assertEquals("GROCERIES", body.string("category"))
        assertEquals("Lima", body.obj("merchant").string("city"))
    }

    @Test
    fun `unknown movement and unknown route are not found`() {
        val token = harness.login()

        assertProblem(harness.get("movements/MOV-404", token), 404, "NOT_FOUND")
        assertProblem(harness.get("movements/MOV-404/breakdown", token), 404, "NOT_FOUND")
        assertProblem(harness.get("cards"), 404, "NOT_FOUND")
    }

    @Test
    fun `every endpoint has a route`() {
        val token = harness.login()

        Endpoint.entries.forEach { endpoint ->
            val response = harness.call(endpoint.method, endpoint.path.replace("{id}", "X"), token = token)
            assertTrue(endpoint.id, response.status in 200..599)
        }
    }

    @Test
    fun `demo reset answers no content and notifies`() {
        val response = harness.call("POST", "_demo/reset")

        assertEquals(204, response.status)
        assertNull(response.body)
        assertEquals(1, harness.resets)
    }

    private fun assertProblem(response: BackendResponse, status: Int, code: String) {
        assertEquals(status, response.status)
        assertEquals(code, response.json().string("code"))
    }
}
