package com.hacybeyker.cartera.feature.biometric.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import com.hacybeyker.cartera.core.testing.FakeBiometricAvailability
import com.hacybeyker.cartera.core.testing.FakeDeviceTokenVault
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.feature.biometric.FakeBiometricEnrollmentRepository
import com.hacybeyker.cartera.feature.biometric.domain.model.EnableBiometricResult
import com.hacybeyker.cartera.feature.biometric.domain.usecase.DisableBiometricLoginUseCase
import com.hacybeyker.cartera.feature.biometric.domain.usecase.EnableBiometricLoginUseCase
import com.hacybeyker.cartera.feature.biometric.domain.usecase.SkipBiometricOfferUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricEnrollmentUseCasesTest {
    private val availability = FakeBiometricAvailability()
    private val vault = FakeDeviceTokenVault()
    private val enrollment = FakeBiometricEnrollmentRepository()
    private val firstRun = FakeFirstRunRepository()
    private val enable = EnableBiometricLoginUseCase(availability, vault, enrollment, firstRun)
    private val disable = DisableBiometricLoginUseCase(vault, enrollment)

    @Test
    fun `enable asks for the fingerprint, registers the device and seals its token`() = runTest {
        assertEquals(EnableBiometricResult.Enabled, enable())

        assertEquals("dvc_1", vault.token)
        assertEquals(1, enrollment.enrolls)
        assertTrue(firstRun.biometricOfferResolved)
    }

    @Test
    fun `without a fingerprint on the device nothing is asked`() = runTest {
        availability.available = false

        assertEquals(EnableBiometricResult.Unavailable, enable())

        assertEquals(0, vault.prompts)
        assertEquals(0, enrollment.enrolls)
    }

    @Test
    fun `cancelling the dialog never calls the server`() = runTest {
        vault.promptFailure = VaultResult.Cancelled

        assertEquals(EnableBiometricResult.Cancelled, enable())

        assertEquals(0, enrollment.enrolls)
        assertNull(vault.token)
        assertFalse(firstRun.biometricOfferResolved)
    }

    @Test
    fun `a key that cannot be created is unavailable`() = runTest {
        listOf(VaultResult.Unavailable, VaultResult.Invalidated).forEach { failure ->
            vault.promptFailure = failure

            assertEquals(EnableBiometricResult.Unavailable, enable())
        }
        assertEquals(0, enrollment.enrolls)
    }

    @Test
    fun `a failed registration discards the authorized seal and keeps nothing`() = runTest {
        enrollment.enrollError = AppError.Network

        assertEquals(EnableBiometricResult.Failure(AppError.Network), enable())

        assertEquals(1, vault.discards)
        assertNull(vault.token)
        assertFalse(firstRun.biometricOfferResolved)
    }

    @Test
    fun `a seal that fails is unavailable`() = runTest {
        vault.sealFails = true

        assertEquals(EnableBiometricResult.Unavailable, enable())

        assertNull(vault.token)
        assertFalse(firstRun.biometricOfferResolved)
    }

    @Test
    fun `disable revokes on the server and then clears the token`() = runTest {
        vault.token = "dvc_1"

        assertEquals(Outcome.Success(Unit), disable())

        assertEquals(1, enrollment.revokes)
        assertNull(vault.token)
    }

    @Test
    fun `a failed revoke keeps the token`() = runTest {
        vault.token = "dvc_1"
        enrollment.revokeError = AppError.Network

        assertEquals(Outcome.Failure(AppError.Network), disable())

        assertEquals("dvc_1", vault.token)
    }

    @Test
    fun `skip resolves the offer`() = runTest {
        SkipBiometricOfferUseCase(firstRun)()

        assertTrue(firstRun.biometricOfferResolved)
    }
}
