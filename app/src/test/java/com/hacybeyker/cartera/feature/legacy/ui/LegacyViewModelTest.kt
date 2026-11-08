package com.hacybeyker.cartera.feature.legacy.ui

import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.feature.legacy.domain.usecase.RegisterBillPaymentUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyViewModelTest {
    private val changes = FakeAccountChanges()
    private val viewModel = LegacyViewModel(RegisterBillPaymentUseCase(changes))

    @Test
    fun `nothing changes until the module reports a payment`() {
        assertEquals(0, changes.revision)
    }

    @Test
    fun `a payment reported by the module invalidates the accounts`() {
        viewModel.onPaid()

        assertEquals(1, changes.revision)
    }
}
