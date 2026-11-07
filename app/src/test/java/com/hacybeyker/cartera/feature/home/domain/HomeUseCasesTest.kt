package com.hacybeyker.cartera.feature.home.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.TestAccounts
import com.hacybeyker.cartera.feature.home.FakeProfileRepository
import com.hacybeyker.cartera.feature.home.TestProfile
import com.hacybeyker.cartera.feature.home.domain.model.Overview
import com.hacybeyker.cartera.feature.home.domain.model.UserProfile
import com.hacybeyker.cartera.feature.home.domain.usecase.GetProfileUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadOverviewUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.LogOutUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeUseCasesTest {
    private val profile = FakeProfileRepository()
    private val accounts = FakeAccountRepository()

    @Test
    fun `overview asks for the profile and the accounts`() = runTest {
        val outcome = LoadOverviewUseCase(profile, accounts)()

        assertEquals(Outcome.Success(Overview(TestProfile, TestAccounts)), outcome)
        assertEquals(1, profile.calls)
        assertEquals(1, accounts.accountsCalls)
    }

    @Test
    fun `overview fails if either call fails`() = runTest {
        profile.error = AppError.Network
        assertEquals(Outcome.Failure(AppError.Network), LoadOverviewUseCase(profile, accounts)())

        profile.error = null
        accounts.error = AppError.Server
        assertEquals(Outcome.Failure(AppError.Server), LoadOverviewUseCase(profile, accounts)())
    }

    @Test
    fun `profile comes from the repository and the greeting uses the first name`() = runTest {
        assertEquals(Outcome.Success(TestProfile), GetProfileUseCase(profile)())
        assertEquals("Ana", TestProfile.firstName)
        assertEquals("Luis", UserProfile("U", "  Luis Ramírez", "", "").firstName)
    }

    @Test
    fun `log out ends the session`() {
        val session = FakeSessionRepository().apply { start("token") }

        LogOutUseCase(session)()

        assertNull(session.accessToken)
    }
}
