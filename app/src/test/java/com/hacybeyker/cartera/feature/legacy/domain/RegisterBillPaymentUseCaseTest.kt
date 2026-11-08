package com.hacybeyker.cartera.feature.legacy.domain

import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.feature.legacy.domain.usecase.RegisterBillPaymentUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class RegisterBillPaymentUseCaseTest {
    private val changes = FakeAccountChanges()

    @Test
    fun `a bill payment raises the revision of the accounts once`() {
        RegisterBillPaymentUseCase(changes)()

        assertEquals(1, changes.revision)
    }

    @Test
    fun `each payment raises it again`() {
        val register = RegisterBillPaymentUseCase(changes)

        register()
        register()

        assertEquals(2, changes.revision)
    }
}
