package com.hacybeyker.cartera.feature.movements.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.FakeMovementRepository
import com.hacybeyker.cartera.core.testing.TestAccounts
import com.hacybeyker.cartera.feature.movements.FakeMovementDetailRepository
import com.hacybeyker.cartera.feature.movements.TestBreakdown
import com.hacybeyker.cartera.feature.movements.TestDetail
import com.hacybeyker.cartera.feature.movements.domain.usecase.GetBreakdownUseCase
import com.hacybeyker.cartera.feature.movements.domain.usecase.GetMovementDetailUseCase
import com.hacybeyker.cartera.feature.movements.domain.usecase.LoadAccountMovementsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MovementsUseCasesTest {
    private val accounts = FakeAccountRepository()
    private val movements = FakeMovementRepository()

    @Test
    fun `account movements bring the header and the first page of that account`() = runTest {
        val result = (LoadAccountMovementsUseCase(accounts, movements)("ACC-002") as Outcome.Success).value

        assertEquals(TestAccounts[1], result.account)
        assertEquals(20, result.firstPage.items.size)
        assertEquals(listOf<String?>("ACC-002"), movements.requestedAccounts)
        assertEquals(listOf<String?>(null), movements.requestedCursors)
    }

    @Test
    fun `an unknown account is not found`() = runTest {
        assertEquals(Outcome.Failure(AppError.NotFound), LoadAccountMovementsUseCase(accounts, movements)("ACC-999"))
    }

    @Test
    fun `a failed first page fails the whole load`() = runTest {
        movements.failures += AppError.Network

        assertEquals(Outcome.Failure(AppError.Network), LoadAccountMovementsUseCase(accounts, movements)("ACC-002"))
    }

    @Test
    fun `detail and breakdown come from the repository`() = runTest {
        val repository = FakeMovementDetailRepository()

        assertEquals(Outcome.Success(TestDetail), GetMovementDetailUseCase(repository)("MOV-002-0001"))
        assertEquals(Outcome.Success(TestBreakdown), GetBreakdownUseCase(repository)("MOV-002-0001"))
        assertEquals(listOf("MOV-002-0001", "MOV-002-0001"), repository.requested)
    }
}
