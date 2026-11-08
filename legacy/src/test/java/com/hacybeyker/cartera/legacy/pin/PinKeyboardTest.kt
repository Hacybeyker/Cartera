package com.hacybeyker.cartera.legacy.pin

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.pin.domain.PinFailure
import com.hacybeyker.cartera.legacy.pin.domain.PinKeyboard
import com.hacybeyker.cartera.legacy.pin.domain.PinNotice
import com.hacybeyker.cartera.legacy.pin.domain.PinPadOrder
import com.hacybeyker.cartera.legacy.pin.domain.toPinFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinKeyboardTest {
    private val keyboard = PinKeyboard()

    private fun rejection(code: ProblemCode) = LegacyError.Rejected(422, code, "detalle", emptyList())

    @Test
    fun `before it starts the order is the fixed one`() {
        assertEquals(PinPadOrder.FIXED, keyboard.order)
    }

    @Test
    fun `it follows the seed and reorders with each rejected pin`() {
        keyboard.start(7)
        assertEquals(PinPadOrder.forSeed(7), keyboard.order)

        keyboard.rejectPin()
        assertEquals(PinPadOrder.forSeed(8), keyboard.order)

        keyboard.rejectPin()
        assertEquals(PinPadOrder.forSeed(9), keyboard.order)
    }

    @Test
    fun `with seed 0 it never reorders`() {
        keyboard.start(0)

        keyboard.rejectPin()
        keyboard.rejectPin()

        assertEquals(PinPadOrder.FIXED, keyboard.order)
    }

    @Test
    fun `a rejected pin drops what was typed`() {
        listOf(1, 2, 3, 4).forEach(keyboard::append)
        assertTrue(keyboard.isFull)

        keyboard.rejectPin()

        assertEquals(0, keyboard.filled)
        assertFalse(keyboard.isFull)
    }

    @Test
    fun `taking the pin empties the keyboard and clearing drops the digits`() {
        listOf(4, 3, 2, 1).forEach(keyboard::append)

        assertEquals("4321", keyboard.takePin())
        assertEquals(0, keyboard.filled)
        keyboard.append(5)
        keyboard.clear()
        assertEquals(0, keyboard.filled)
        assertFalse(keyboard.deleteLast())
    }

    @Test
    fun `each error of the payment maps to what the screen does`() {
        assertEquals(PinFailure.SessionExpired, LegacyError.SessionExpired.toPinFailure())
        assertEquals(PinFailure.WrongPin, rejection(ProblemCode.INVALID_PIN).toPinFailure())
        assertEquals(
            PinFailure.Rejected(PinNotice.INSUFFICIENT_FUNDS),
            rejection(ProblemCode.INSUFFICIENT_FUNDS).toPinFailure()
        )
        assertEquals(
            PinFailure.Rejected(PinNotice.DEBT_ALREADY_PAID),
            rejection(ProblemCode.DEBT_ALREADY_PAID).toPinFailure()
        )
        listOf(ProblemCode.VALIDATION_ERROR, ProblemCode.CURRENCY_MISMATCH, ProblemCode.UNKNOWN).forEach {
            assertEquals(PinFailure.Rejected(PinNotice.REJECTED), rejection(it).toPinFailure())
        }
        assertEquals(PinFailure.Retryable(LegacyError.Network), LegacyError.Network.toPinFailure())
        assertEquals(PinFailure.Retryable(LegacyError.Server), LegacyError.Server.toPinFailure())
    }
}
