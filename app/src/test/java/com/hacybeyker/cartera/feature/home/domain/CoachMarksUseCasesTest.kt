package com.hacybeyker.cartera.feature.home.domain

import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.feature.home.domain.model.CoachMark
import com.hacybeyker.cartera.feature.home.domain.usecase.CompleteCoachMarksUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.GetPendingCoachMarkUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachMarksUseCasesTest {
    @Test
    fun `steps go transfer, market tab and menu, numbered from one`() {
        assertEquals(CoachMark.TRANSFER, CoachMark.first)
        assertEquals(CoachMark.MARKET_TAB, CoachMark.TRANSFER.next())
        assertEquals(CoachMark.MENU, CoachMark.MARKET_TAB.next())
        assertNull(CoachMark.MENU.next())
        assertEquals(listOf(1, 2, 3), CoachMark.entries.map { it.number })
        assertEquals(listOf(false, false, true), CoachMark.entries.map { it.isLast })
    }

    @Test
    fun `a fresh install starts at the first step`() = runTest {
        assertEquals(CoachMark.TRANSFER, GetPendingCoachMarkUseCase(FakeFirstRunRepository())())
    }

    @Test
    fun `once completed there is nothing pending`() = runTest {
        val firstRun = FakeFirstRunRepository()

        CompleteCoachMarksUseCase(firstRun)()

        assertTrue(firstRun.coachMarksSeen)
        assertNull(GetPendingCoachMarkUseCase(firstRun)())
    }

    @Test
    fun `completing does not touch the other first run flags`() = runTest {
        val firstRun = FakeFirstRunRepository()

        CompleteCoachMarksUseCase(firstRun)()

        assertFalse(firstRun.onboardingSeen)
        assertFalse(firstRun.biometricOfferResolved)
    }
}
