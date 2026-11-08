package com.hacybeyker.cartera.feature.login.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.testing.FakeAppConfigRepository
import com.hacybeyker.cartera.core.testing.FakeDeviceTokenVault
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.TestAppConfig
import com.hacybeyker.cartera.core.testing.resolveEntryStep
import com.hacybeyker.cartera.feature.login.FakeAuthRepository
import com.hacybeyker.cartera.feature.login.domain.model.BiometricDisabledReason
import com.hacybeyker.cartera.feature.login.domain.model.BiometricLoginResult
import com.hacybeyker.cartera.feature.login.domain.usecase.LogInWithBiometricsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogInWithBiometricsUseCaseTest {
    private val vault = FakeDeviceTokenVault(token = "dvc_1")
    private val auth = FakeAuthRepository()
    private val session = FakeSessionRepository()
    private val appConfig = FakeAppConfigRepository()
    private val firstRun = FakeFirstRunRepository()
    private val logIn = LogInWithBiometricsUseCase(vault, auth, session, resolveEntryStep(appConfig, firstRun))

    @Test
    fun `the fingerprint opens the token and starts the same in-memory session`() = runTest {
        assertEquals(BiometricLoginResult.LoggedIn(EntryStep.TERMS), logIn())

        assertEquals(listOf("dvc_1"), auth.deviceTokens)
        assertEquals("jwt-device-1", session.accessToken)
    }

    @Test
    fun `a returning user goes straight home`() = runTest {
        firstRun.acceptedTermsVersion = TestAppConfig.termsVersion
        firstRun.onboardingSeen = true

        assertEquals(BiometricLoginResult.LoggedIn(EntryStep.HOME), logIn())
    }

    @Test
    fun `cancelling or a dialog that cannot show changes nothing`() = runTest {
        listOf(VaultResult.Cancelled, VaultResult.Unavailable).forEach { failure ->
            vault.promptFailure = failure

            assertEquals(BiometricLoginResult.Cancelled, logIn())
        }

        assertTrue(auth.deviceTokens.isEmpty())
        assertEquals("dvc_1", vault.token)
    }

    @Test
    fun `an invalidated key disables the biometric login`() = runTest {
        vault.keyInvalidated = true

        assertEquals(BiometricLoginResult.Disabled(BiometricDisabledReason.KEY_INVALIDATED), logIn())
        assertTrue(auth.deviceTokens.isEmpty())
    }

    @Test
    fun `invalid device clears the token and disables the biometric login`() = runTest {
        auth.error = AppError.InvalidDevice

        assertEquals(BiometricLoginResult.Disabled(BiometricDisabledReason.DEVICE_REVOKED), logIn())

        assertNull(vault.token)
        assertNull(session.accessToken)
    }

    @Test
    fun `other failures keep the token`() = runTest {
        auth.error = AppError.Network

        assertEquals(BiometricLoginResult.Failure(AppError.Network), logIn())

        assertEquals("dvc_1", vault.token)
        assertEquals(0, vault.clears)
    }

    @Test
    fun `a config failure after the login is a failure`() = runTest {
        appConfig.error = AppError.Server

        assertEquals(BiometricLoginResult.Failure(AppError.Server), logIn())
    }
}
