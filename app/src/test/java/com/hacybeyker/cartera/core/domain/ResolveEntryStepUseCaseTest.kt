package com.hacybeyker.cartera.core.domain

import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.domain.entry.ResolveEntryStepUseCase
import com.hacybeyker.cartera.core.testing.FakeAppConfigRepository
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.TestAppConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveEntryStepUseCaseTest {
    private val appConfig = FakeAppConfigRepository()
    private val firstRun = FakeFirstRunRepository()
    private val resolve = ResolveEntryStepUseCase(appConfig, firstRun)

    @Test
    fun `terms never accepted go to terms`() = runTest {
        assertEquals(Outcome.Success(EntryStep.TERMS), resolve())
    }

    @Test
    fun `terms accepted in an older version go to terms again`() = runTest {
        firstRun.acceptedTermsVersion = "2025-01-01"
        firstRun.onboardingSeen = true

        assertEquals(Outcome.Success(EntryStep.TERMS), resolve())
    }

    @Test
    fun `current terms without onboarding go to onboarding`() = runTest {
        firstRun.acceptedTermsVersion = TestAppConfig.termsVersion

        assertEquals(Outcome.Success(EntryStep.ONBOARDING), resolve())
    }

    @Test
    fun `current terms and onboarding seen go home`() = runTest {
        firstRun.acceptedTermsVersion = TestAppConfig.termsVersion
        firstRun.onboardingSeen = true

        assertEquals(Outcome.Success(EntryStep.HOME), resolve())
    }

    @Test
    fun `config failure is returned as is`() = runTest {
        appConfig.error = AppError.Network

        assertEquals(Outcome.Failure(AppError.Network), resolve())
    }
}
