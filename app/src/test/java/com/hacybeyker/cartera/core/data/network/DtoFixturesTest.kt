package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.network.dto.AccountDto
import com.hacybeyker.cartera.core.data.network.dto.AppConfigDto
import com.hacybeyker.cartera.core.data.network.dto.BreakdownDto
import com.hacybeyker.cartera.core.data.network.dto.MeDto
import com.hacybeyker.cartera.core.data.network.dto.MovementDetailDto
import com.hacybeyker.cartera.core.data.network.dto.MovementDto
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoFixturesTest {
    private val fixtures = File(requireNotNull(System.getProperty("cartera.fixturesDir")))
    private val json = CarteraHttpClient.json
    private val movementFiles = listOf("ACC-001", "ACC-002", "ACC-003").map { "movements/$it.json" }

    @Test
    fun `app config decodes`() {
        assertEquals(
            AppConfigDto("0.1.0", "2026-11-01"),
            json.decodeFromJsonElement(AppConfigDto.serializer(), read("app-config.json"))
        )
    }

    @Test
    fun `users decode as the profile`() {
        val users = array("users.json").map { json.decodeFromJsonElement(MeDto.serializer(), it) }

        assertEquals("Ana Torres", users.single().fullName)
    }

    @Test
    fun `accounts decode with their balances`() {
        val accounts = array("accounts.json").map { json.decodeFromJsonElement(AccountDto.serializer(), it) }

        assertEquals(listOf("8450.75", "2300.00", "1050.20"), accounts.map { it.availableBalance.value })
    }

    @Test
    fun `every movement decodes as summary, detail and breakdown`() {
        val records = movementFiles.flatMap(::array)

        records.forEach { record ->
            json.decodeFromJsonElement(MovementDto.serializer(), record)
            json.decodeFromJsonElement(MovementDetailDto.serializer(), record)
            json.decodeFromJsonElement(BreakdownDto.serializer(), record.jsonObject.getValue("breakdown"))
        }
        assertEquals(1242, records.size)
    }

    @Test
    fun `optional breakdown fields decode when present and when absent`() {
        val breakdowns = movementFiles.flatMap(::array)
            .map { json.decodeFromJsonElement(BreakdownDto.serializer(), it.jsonObject.getValue("breakdown")) }

        assertTrue(breakdowns.any { it.exchangeRate == "3.7520" })
        assertTrue(breakdowns.any { it.merchant == null })
        assertNotNull(breakdowns.first().merchant)
        assertNull(breakdowns.first().exchangeRate)
    }

    @Test
    fun `strict decoding proves every dto field name exists in the fixtures`() {
        val strict = Json { ignoreUnknownKeys = false }
        val record = array("movements/ACC-002.json").first().jsonObject

        strict.decodeFromJsonElement(BreakdownDto.serializer(), record.getValue("breakdown"))
        strict.decodeFromJsonElement(AppConfigDto.serializer(), read("app-config.json"))
    }

    private fun read(path: String): JsonElement = Json.parseToJsonElement(File(fixtures, path).readText())

    private fun array(path: String) = read(path).jsonArray.toList()
}
