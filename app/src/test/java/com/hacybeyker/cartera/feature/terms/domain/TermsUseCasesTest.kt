package com.hacybeyker.cartera.feature.terms.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.testing.FakeAppConfigRepository
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.TestAppConfig
import com.hacybeyker.cartera.core.testing.resolveEntryStep
import com.hacybeyker.cartera.feature.terms.domain.usecase.AcceptTermsUseCase
import com.hacybeyker.cartera.feature.terms.domain.usecase.RejectTermsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TermsUseCasesTest {
    private val appConfig = FakeAppConfigRepository()
    private val firstRun = FakeFirstRunRepository()
    private val accept = AcceptTermsUseCase(appConfig, firstRun, resolveEntryStep(appConfig, firstRun))

    @Test
    fun `accepting saves the current version and goes to onboarding the first time`() = runTest {
        assertEquals(Outcome.Success(EntryStep.ONBOARDING), accept())
        assertEquals(TestAppConfig.termsVersion, firstRun.acceptedTermsVersion)
    }

    @Test
    fun `accepting new terms after the onboarding goes home`() = runTest {
        firstRun.onboardingSeen = true

        assertEquals(Outcome.Success(EntryStep.HOME), accept())
    }

    @Test
    fun `without config nothing is saved`() = runTest {
        appConfig.error = AppError.Network

        assertEquals(Outcome.Failure(AppError.Network), accept())
        assertNull(firstRun.acceptedTermsVersion)
    }

    @Test
    fun `rejecting ends the session`() {
        val session = FakeSessionRepository().apply { start("jwt") }

        RejectTermsUseCase(session)()

        assertNull(session.accessToken)
    }
}
