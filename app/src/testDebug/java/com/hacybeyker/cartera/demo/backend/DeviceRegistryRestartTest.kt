package com.hacybeyker.cartera.demo.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** El registro de dispositivos sobrevive a un proceso nuevo (otro backend con el mismo almacenamiento). */
class DeviceRegistryRestartTest {
    private val devices = InMemoryDeviceRegistry()

    @Test
    fun `a backend in a new process accepts the device token issued by the previous one`() {
        val deviceToken = RouterHarness(devices = devices).enroll()

        assertEquals(200, RouterHarness(devices = devices).biometricLogin(deviceToken).status)
    }

    @Test
    fun `without a shared registry a new process does not know the device`() {
        val deviceToken = RouterHarness().enroll()

        assertEquals("INVALID_DEVICE", RouterHarness().biometricLogin(deviceToken).problemCode())
    }

    @Test
    fun `reset forgets the device for the next processes too`() {
        val deviceToken = RouterHarness(devices = devices).enroll()

        RouterHarness(devices = devices).call("POST", "_demo/reset")

        assertEquals("INVALID_DEVICE", RouterHarness(devices = devices).biometricLogin(deviceToken).problemCode())
        assertNull(devices.userOf(deviceToken))
    }

    @Test
    fun `delete revokes the device for the next processes too`() {
        val deviceToken = RouterHarness(devices = devices).enroll()

        val revoking = RouterHarness(devices = devices)
        assertEquals(204, revoking.call("DELETE", "auth/biometric", token = revoking.login()).status)

        assertEquals("INVALID_DEVICE", RouterHarness(devices = devices).biometricLogin(deviceToken).problemCode())
    }

    @Test
    fun `the in-memory registry revokes only the devices of the user`() {
        devices.register("dvc_ana", "USR-001")
        devices.register("dvc_otro", "USR-002")

        devices.revokeAll("USR-001")

        assertNull(devices.userOf("dvc_ana"))
        assertEquals("USR-002", devices.userOf("dvc_otro"))
    }

    private fun RouterHarness.enroll(): String =
        call("POST", "auth/biometric/enroll", token = login()).json().string("deviceToken")

    private fun RouterHarness.biometricLogin(deviceToken: String) =
        call(method = "POST", path = "auth/biometric/login", body = """{"deviceToken":"$deviceToken"}""")
}
