package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixturesTest {
    private val catalog = FixtureCatalog(fileFixtures)
    private val accountIds = listOf("ACC-001", "ACC-002", "ACC-003")

    @Test
    fun `movement counts match the reference data`() {
        assertEquals(listOf(1200, 30, 12), accountIds.map { catalog.movements(it).size })
    }

    @Test
    fun `movement ids are unique and belong to their file`() {
        val all = accountIds.flatMap { id -> catalog.movements(id).onEach { assertEquals(id, it.accountId) } }

        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test
    fun `every breakdown adds up to the movement amount`() {
        accountIds.flatMap(catalog::movements).forEach { record ->
            val breakdown = record.json.obj("breakdown")
            val lines = breakdown.getValue("lines").jsonArray.sumOf { value(it.jsonObject.obj("amount")) }
            val total = lines + value(breakdown.obj("fees")) + value(breakdown.obj("taxes"))

            assertEquals(record.id, value(record.json.obj("amount")).abs(), total)
        }
    }

    @Test
    fun `amount sign matches the movement type`() {
        accountIds.flatMap(catalog::movements).forEach { record ->
            val negative = value(record.json.obj("amount")).signum() < 0
            assertEquals(record.id, if (negative) "DEBIT" else "CREDIT", record.json.string("type"))
        }
    }

    @Test
    fun `third party accounts cover both currencies`() {
        assertEquals(
            listOf("PEN", "PEN", "USD"),
            catalog.thirdPartyAccounts.map { it.string("currency") }
        )
        assertTrue(catalog.users.single().string("username") == "ana.torres")
    }

    private fun value(money: JsonObject) = BigDecimal(money.string("value"))
}
