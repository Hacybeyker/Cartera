package com.hacybeyker.cartera.demo.backend

/**
 * Dispositivos registrados para ingresar con huella: `deviceToken` → id de quien lo registró. Es lo único del estado
 * del backend que sobrevive a un proceso nuevo, porque el ingreso con huella sirve justamente en los arranques
 * siguientes.
 */
interface DeviceRegistry {
    fun userOf(deviceToken: String): String?

    fun register(deviceToken: String, userId: String)

    /** Revoca todos los dispositivos de [userId]. */
    fun revokeAll(userId: String)

    fun clear()
}

class InMemoryDeviceRegistry : DeviceRegistry {
    private val devices = mutableMapOf<String, String>()

    override fun userOf(deviceToken: String): String? = devices[deviceToken]

    override fun register(deviceToken: String, userId: String) {
        devices[deviceToken] = userId
    }

    override fun revokeAll(userId: String) {
        devices.values.removeAll { it == userId }
    }

    override fun clear() = devices.clear()
}
