package com.hacybeyker.cartera.core.data.pinpad

import org.junit.Assert.assertEquals
import org.junit.Test

class SourcedPinPadSeedTest {

    @Test
    fun `without sources, as in release, the order is fixed`() {
        assertEquals(0, SourcedPinPadSeed(emptySet()).current())
    }

    @Test
    fun `the first source that decides wins`() {
        val undecided = PinPadSeedSource { null }
        val scenario = PinPadSeedSource { 7 }
        val other = PinPadSeedSource { 9 }

        assertEquals(7, SourcedPinPadSeed(linkedSetOf(undecided, scenario, other)).current())
    }

    @Test
    fun `sources that never decide leave the fixed order`() {
        assertEquals(0, SourcedPinPadSeed(setOf(PinPadSeedSource { null })).current())
    }
}
