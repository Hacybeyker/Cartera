package com.hacybeyker.cartera.legacy.integration

/** Interruptores del cliente que la app decide. Cada consulta se hace en el momento de usarse, sin guardarla. */
interface LegacyFeatureFlags {
    fun billersFullRebind(): Boolean

    fun debtsMainDecode(): Boolean
}
