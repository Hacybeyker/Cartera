package com.hacybeyker.cartera.core.domain.biometric

import com.hacybeyker.cartera.core.testing.FakeBiometricAvailability
import com.hacybeyker.cartera.core.testing.FakeDeviceTokenVault
import com.hacybeyker.cartera.core.testing.biometricStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricStatusTest {
    private val availability = FakeBiometricAvailability()
    private val vault = FakeDeviceTokenVault()
    private val getStatus = biometricStatus(availability, vault)

    @Test
    fun `enabled needs a fingerprint on the device and a token the key can open`() = runTest {
        assertFalse(getStatus().isEnabled)

        vault.token = "dvc_1"
        assertTrue(getStatus().isEnabled)

        availability.available = false
        assertEquals(BiometricStatus(isAvailable = false, token = DeviceTokenState.READY), getStatus())
        assertFalse(getStatus().isEnabled)
    }

    @Test
    fun `an invalidated key is reported and is not enabled`() = runTest {
        vault.token = "dvc_1"
        vault.keyInvalidated = true

        val status = getStatus()

        assertEquals(DeviceTokenState.INVALIDATED, status.token)
        assertFalse(status.isEnabled)
    }
}
