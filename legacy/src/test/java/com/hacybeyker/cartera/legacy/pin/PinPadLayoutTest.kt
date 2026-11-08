package com.hacybeyker.cartera.legacy.pin

import com.hacybeyker.cartera.legacy.pin.domain.PinBounds
import com.hacybeyker.cartera.legacy.pin.domain.PinKey
import com.hacybeyker.cartera.legacy.pin.domain.PinPadLayout
import com.hacybeyker.cartera.legacy.pin.domain.PinPadOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PinPadLayoutTest {
    private val width = 300
    private val height = 400
    private val fixed = PinPadLayout(PinPadOrder.FIXED)

    private fun digitAt(layout: PinPadLayout, x: Int, y: Int): Int? =
        (layout.slotAt(x, y, width, height)?.key as? PinKey.Digit)?.value

    @Test
    fun `three rows of three digits and, below, a gap, the last digit and delete`() {
        val layout = PinPadLayout(PinPadOrder.forSeed(7))

        val grid = layout.slots.map { Triple(it.row, it.column, it.key) }

        assertEquals(
            listOf(
                Triple(0, 0, PinKey.Digit(1)),
                Triple(0, 1, PinKey.Digit(2)),
                Triple(0, 2, PinKey.Digit(0)),
                Triple(1, 0, PinKey.Digit(4)),
                Triple(1, 1, PinKey.Digit(8)),
                Triple(1, 2, PinKey.Digit(5)),
                Triple(2, 0, PinKey.Digit(9)),
                Triple(2, 1, PinKey.Digit(6)),
                Triple(2, 2, PinKey.Digit(3)),
                Triple(3, 1, PinKey.Digit(7)),
                Triple(3, 2, PinKey.Delete)
            ),
            grid
        )
    }

    @Test
    fun `the ids follow the reading order`() {
        assertEquals((0..10).toList(), fixed.slots.map { it.id })
        assertEquals(PinKey.Delete, fixed.slot(10)?.key)
        assertEquals(PinKey.Digit(0), fixed.slot(9)?.key)
        assertNull(fixed.slot(11))
        assertNull(fixed.slot(-1))
    }

    @Test
    fun `the cells tile the view without gaps or overlaps`() {
        val cells = fixed.slots.map { fixed.boundsOf(it, width, height) }

        assertEquals(PinBounds(0, 0, 100, 100), cells[0])
        assertEquals(PinBounds(100, 0, 200, 100), cells[1])
        assertEquals(PinBounds(200, 300, 300, 400), cells[10])
        for (x in 0 until width) {
            for (y in 0 until height) {
                assertTrue(cells.count { it.contains(x, y) } <= 1)
            }
        }
    }

    @Test
    fun `the cells keep tiling when the size is not a multiple of the grid`() {
        val cells = fixed.slots.associateBy({ it.row to it.column }) { fixed.boundsOf(it, 301, 401) }

        assertEquals(301, cells.getValue(0 to 2).right)
        assertEquals(401, cells.getValue(3 to 2).bottom)
        assertEquals(cells.getValue(0 to 0).right, cells.getValue(0 to 1).left)
        assertEquals(cells.getValue(0 to 1).right, cells.getValue(0 to 2).left)
        assertEquals(cells.getValue(0 to 0).bottom, cells.getValue(1 to 0).top)
        assertEquals(cells.getValue(2 to 0).bottom, cells.getValue(3 to 1).top)
    }

    @Test
    fun `a touch resolves to the key under it with the fixed order`() {
        assertEquals(1, digitAt(fixed, 50, 50))
        assertEquals(2, digitAt(fixed, 150, 50))
        assertEquals(3, digitAt(fixed, 250, 50))
        assertEquals(5, digitAt(fixed, 150, 150))
        assertEquals(9, digitAt(fixed, 250, 250))
        assertEquals(0, digitAt(fixed, 150, 350))
        assertEquals(PinKey.Delete, fixed.slotAt(250, 350, width, height)?.key)
    }

    @Test
    fun `a touch resolves with the shuffled order, never with the fixed one`() {
        val layout = PinPadLayout(PinPadOrder.forSeed(7))

        assertEquals(1, digitAt(layout, 50, 50))
        assertEquals(0, digitAt(layout, 250, 50))
        assertEquals(8, digitAt(layout, 150, 150))
        assertEquals(7, digitAt(layout, 150, 350))
    }

    @Test
    fun `the gap, the borders and what is outside hit nothing`() {
        assertNull(fixed.slotAt(50, 350, width, height))
        assertNull(fixed.slotAt(-1, 50, width, height))
        assertNull(fixed.slotAt(50, -1, width, height))
        assertNull(fixed.slotAt(width, 50, width, height))
        assertNull(fixed.slotAt(50, height, width, height))
        assertEquals(1, digitAt(fixed, 0, 0))
        assertEquals(PinKey.Delete, fixed.slotAt(width - 1, height - 1, width, height)?.key)
    }

    @Test
    fun `the center of a key bounds hits that same key`() {
        val layout = PinPadLayout(PinPadOrder.forSeed(7))

        layout.slots.forEach { slot ->
            val bounds = layout.boundsOf(slot, width, height)
            assertEquals(
                slot,
                layout.slotAt((bounds.left + bounds.right) / 2, (bounds.top + bounds.bottom) / 2, width, height)
            )
        }
    }

    @Test
    fun `an order without the ten digits is rejected`() {
        listOf(listOf(1, 2, 3), (1..10).toList(), listOf(1, 1, 2, 3, 4, 5, 6, 7, 8, 9)).forEach { order ->
            try {
                PinPadLayout(order)
                fail("Expected $order to be rejected")
            } catch (expected: IllegalArgumentException) {
                assertFalse(expected.message.isNullOrBlank())
            }
        }
    }
}
