package com.hacybeyker.cartera.legacy.debts

import com.hacybeyker.cartera.legacy.core.domain.Money
import com.hacybeyker.cartera.legacy.core.domain.PEN
import com.hacybeyker.cartera.legacy.debts.domain.DebtSelection
import com.hacybeyker.cartera.legacy.debts.domain.label
import com.hacybeyker.cartera.legacy.debts.domain.total
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.pen
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtSelectionTest {
    private val selection = DebtSelection(Samples.luzDebts.debts)

    @Test
    fun `nothing is selected at first and the total is zero soles`() {
        assertTrue(selection.selected.isEmpty())
        assertEquals(Money.zero(PEN), selection.total)
        assertFalse(selection.isSelected(Samples.octubre.id))
    }

    @Test
    fun `selected debts add up exactly`() {
        val both = selection.toggle(Samples.octubre.id).toggle(Samples.noviembre.id)

        assertEquals(listOf(Samples.octubre, Samples.noviembre), both.selected)
        assertEquals(pen("177.50"), both.total)
    }

    @Test
    fun `toggling a selected debt removes it and leaves the original selection unchanged`() {
        val one = selection.toggle(Samples.octubre.id)
        val none = one.toggle(Samples.octubre.id)

        assertEquals(pen("85.40"), one.total)
        assertEquals(Money.zero(PEN), none.total)
        assertTrue(one.isSelected(Samples.octubre.id))
    }

    @Test
    fun `an id that is not a pending debt changes nothing`() {
        assertEquals(selection, selection.toggle("DEB-OTRO"))
    }

    @Test
    fun `the total of a list of debts is zero soles when it is empty`() {
        assertEquals(Money.zero(PEN), emptyList<com.hacybeyker.cartera.legacy.debts.domain.Debt>().total())
    }

    @Test
    fun `periods and due dates are labelled in spanish whatever the device locale`() {
        assertEquals("Octubre 2026", YearMonth.of(2026, 10).label())
        assertEquals("Enero 2027", YearMonth.of(2027, 1).label())
        assertEquals("12/11/2026", LocalDate.of(2026, 11, 12).label())
        assertEquals("05/01/2027", LocalDate.of(2027, 1, 5).label())
    }
}
