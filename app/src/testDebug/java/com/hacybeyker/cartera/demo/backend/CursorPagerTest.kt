package com.hacybeyker.cartera.demo.backend

import java.time.Instant
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CursorPagerTest {
    private val records = (1..5).map { record("MOV-$it", Instant.parse("2026-11-0${6 - it}T10:00:00Z")) }

    @Test
    fun `pages walk the list until the cursor runs out`() {
        val first = CursorPager.page(records, cursor = null, limit = 2)
        val second = CursorPager.page(records, first.nextCursor, limit = 2)
        val third = CursorPager.page(records, second.nextCursor, limit = 2)

        assertEquals(listOf("MOV-1", "MOV-2"), first.items.map { it.id })
        assertEquals(listOf("MOV-3", "MOV-4"), second.items.map { it.id })
        assertEquals(listOf("MOV-5"), third.items.map { it.id })
        assertNull(third.nextCursor)
    }

    @Test
    fun `exact last page has no next cursor`() {
        assertNull(CursorPager.page(records, cursor = null, limit = 5).nextCursor)
    }

    @Test
    fun `new item at the top does not shift the next page`() {
        val first = CursorPager.page(records, cursor = null, limit = 2)
        val withNewest = listOf(record("MOV-NEW", Instant.parse("2026-11-07T10:00:00Z"))) + records

        val second = CursorPager.page(withNewest, first.nextCursor, limit = 2)

        assertEquals(listOf("MOV-3", "MOV-4"), second.items.map { it.id })
    }

    @Test
    fun `unknown or malformed cursor is a validation error`() {
        val unknown = assertThrows(ProblemException::class.java) {
            CursorPager.page(records, CursorPager.encode("MOV-404"), limit = 2)
        }
        val malformed = assertThrows(ProblemException::class.java) {
            CursorPager.page(records, "%%%", limit = 2)
        }

        assertEquals(ProblemCode.VALIDATION_ERROR, unknown.code)
        assertEquals("cursor", malformed.errors.single().field)
    }

    private fun record(id: String, instant: Instant) = MovementRecord(id, "ACC-001", instant, JsonObject(emptyMap()))
}
