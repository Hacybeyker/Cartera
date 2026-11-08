package com.hacybeyker.cartera.core.domain

import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.testing.FakeAppConfigRepository
import com.hacybeyker.cartera.core.testing.FakeBiometricAvailability
import com.hacybeyker.cartera.core.testing.FakeDeviceTokenVault
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.TestAppConfig
import com.hacybeyker.cartera.core.testing.resolveEntryStep
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveEntryStepUseCaseTest {
    private val appConfig = FakeAppConfigRepository()
    private val firstRun = FakeFirstRunRepository()
    private val availability = FakeBiometricAvailability(available = false)
    private val vault = FakeDeviceTokenVault()
    private val resolve = resolveEntryStep(appConfig, firstRun, availability, vault)

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
        seenEverything()

        assertEquals(Outcome.Success(EntryStep.HOME), resolve())
    }

    @Test
    fun `a device with a fingerprint and nothing registered gets the offer after the onboarding`() = runTest {
        seenEverything()
        availability.available = true

        assertEquals(Outcome.Success(EntryStep.BIOMETRIC_OFFER), resolve())
        assertEquals(EntryStep.BIOMETRIC_OFFER, resolve.afterOnboarding())
    }

    @Test
    fun `the offer is not shown once resolved`() = runTest {
        seenEverything()
        availability.available = true
        firstRun.biometricOfferResolved = true

        assertEquals(Outcome.Success(EntryStep.HOME), resolve())
    }

    @Test
    fun `the offer is not shown when the device is already registered`() = runTest {
        seenEverything()
        availability.available = true
        vault.token = "dvc_1"

        assertEquals(Outcome.Success(EntryStep.HOME), resolve())
    }

    @Test
    fun `the offer is not shown without a fingerprint`() = runTest {
        assertEquals(EntryStep.HOME, resolve.afterOnboarding())
    }

    @Test
    fun `config failure is returned as is`() = runTest {
        appConfig.error = AppError.Network

        assertEquals(Outcome.Failure(AppError.Network), resolve())
    }

    private fun seenEverything() {
        firstRun.acceptedTermsVersion = TestAppConfig.termsVersion
        firstRun.onboardingSeen = true
    }
}
