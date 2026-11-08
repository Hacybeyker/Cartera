package com.hacybeyker.cartera.legacy.pin

import com.hacybeyker.cartera.legacy.pin.domain.PinPadOrder
import java.util.Collections
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PinPadOrderTest {

    @Test
    fun `seed 0 keeps the fixed order, 1 to 9 and then 0`() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0), PinPadOrder.forSeed(0))
        assertEquals(PinPadOrder.FIXED, PinPadOrder.forSeed(0))
    }

    @Test
    fun `seed 7 gives this permutation, the one the verification flows rely on`() {
        assertEquals(listOf(1, 2, 0, 4, 8, 5, 9, 6, 3, 7), PinPadOrder.forSeed(7))
    }

    @Test
    fun `other seeds give their own permutation, the same on every run`() {
        assertEquals(listOf(7, 0, 8, 9, 5, 3, 1, 4, 2, 6), PinPadOrder.forSeed(1))
        assertEquals(listOf(6, 2, 3, 9, 4, 7, 1, 0, 8, 5), PinPadOrder.forSeed(8))
        assertEquals(PinPadOrder.forSeed(42), PinPadOrder.forSeed(42))
    }

    @Test
    fun `the algorithm is Fisher-Yates over java util Random, the same as Collections shuffle`() {
        val shuffled = PinPadOrder.FIXED.toMutableList().also { Collections.shuffle(it, Random(7)) }

        assertEquals(shuffled, PinPadOrder.forSeed(7))
    }

    @Test
    fun `every seed uses each digit exactly once`() {
        (1..200).forEach { seed ->
            assertEquals((0..9).toList(), PinPadOrder.forSeed(seed).sorted())
        }
    }

    @Test
    fun `after a rejected pin the order follows the seed plus the rejections`() {
        assertEquals(PinPadOrder.forSeed(7), PinPadOrder.afterRejections(7, 0))
        assertEquals(PinPadOrder.forSeed(8), PinPadOrder.afterRejections(7, 1))
        assertEquals(PinPadOrder.forSeed(9), PinPadOrder.afterRejections(7, 2))
        assertNotEquals(PinPadOrder.afterRejections(7, 0), PinPadOrder.afterRejections(7, 1))
    }

    @Test
    fun `with seed 0 the order stays fixed after any number of rejections`() {
        assertEquals(PinPadOrder.FIXED, PinPadOrder.afterRejections(0, 0))
        assertEquals(PinPadOrder.FIXED, PinPadOrder.afterRejections(0, 5))
    }
}
