package com.hacybeyker.cartera.feature.exchange.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.feature.exchange.FakeExchangeRepository
import com.hacybeyker.cartera.feature.exchange.ReferenceExchange
import com.hacybeyker.cartera.feature.exchange.ReferenceExchangeReceipt
import com.hacybeyker.cartera.feature.exchange.domain.usecase.ConfirmExchangeUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfirmExchangeUseCaseTest {

    @Test
    fun `a done exchange, or one that may have been done, invalidates the accounts`() = runTest {
        listOf(null, AppError.Network, AppError.Server).forEach { failure ->
            val exchanges = FakeExchangeRepository().apply { failure?.let { failures += it } }
            val changes = FakeAccountChanges()

            val outcome = ConfirmExchangeUseCase(exchanges, changes)(ReferenceExchange)

            assertEquals(failure?.let { Outcome.Failure(it) } ?: Outcome.Success(ReferenceExchangeReceipt), outcome)
            assertEquals(1, changes.revision)
        }
    }

    @Test
    fun `a rejected or expired exchange invalidates nothing`() = runTest {
        val rejections = listOf(
            AppError.InsufficientFunds,
            AppError.CurrencyMismatch,
            AppError.QuoteExpired(null),
            AppError.Validation(mapOf("amount.value" to "TOO_SMALL")),
            AppError.SessionExpired
        )
        rejections.forEach { error ->
            val changes = FakeAccountChanges()

            ConfirmExchangeUseCase(FakeExchangeRepository().apply { failures += error }, changes)(ReferenceExchange)

            assertEquals(0, changes.revision)
        }
    }
}
