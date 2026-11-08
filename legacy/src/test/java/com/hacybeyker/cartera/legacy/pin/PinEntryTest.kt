package com.hacybeyker.cartera.legacy.pin

import com.hacybeyker.cartera.legacy.pin.domain.PinEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinEntryTest {
    private val entry = PinEntry()

    @Test
    fun `digits pile up until the fourth, which completes the pin`() {
        assertTrue(entry.append(1))
        assertTrue(entry.append(2))
        assertTrue(entry.append(3))
        assertFalse(entry.isFull)
        assertTrue(entry.append(4))

        assertTrue(entry.isFull)
        assertEquals(4, entry.size)
    }

    @Test
    fun `a fifth digit is ignored`() {
        listOf(1, 2, 3, 4).forEach(entry::append)

        assertFalse(entry.append(5))

        assertEquals("1234", entry.take())
    }

    @Test
    fun `a value that is not a digit is ignored`() {
        assertFalse(entry.append(10))
        assertFalse(entry.append(-1))
        assertEquals(0, entry.size)
    }

    @Test
    fun `deleting removes the last digit and does nothing when empty`() {
        assertFalse(entry.deleteLast())
        entry.append(1)
        entry.append(2)

        assertTrue(entry.deleteLast())

        assertEquals(1, entry.size)
        assertEquals("1", entry.take())
    }

    @Test
    fun `taking the pin hands it over once and leaves the entry empty`() {
        listOf(1, 2, 3, 4).forEach(entry::append)

        assertEquals("1234", entry.take())

        assertEquals(0, entry.size)
        assertEquals("", entry.take())
    }

    @Test
    fun `clearing drops what was typed`() {
        entry.append(7)

        entry.clear()

        assertEquals(0, entry.size)
    }
}
