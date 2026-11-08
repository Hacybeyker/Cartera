package com.hacybeyker.cartera.core.domain.biometric

/**
 * Guarda el `deviceToken` cifrado con una clave que solo la huella desbloquea. Cada operación que lo abre o lo
 * guarda muestra el diálogo del sistema.
 */
interface DeviceTokenVault {
    /** Sin diálogo. Si la clave quedó invalidada, borra lo guardado y responde [DeviceTokenState.INVALIDATED]. */
    suspend fun state(): DeviceTokenState

    /** Pide la huella antes de tener el token: si se cancela, no se llamó al servidor ni quedó nada guardado. */
    suspend fun unlockForSealing(): VaultResult<PendingSeal>

    /** Pide la huella y devuelve el token descifrado. */
    suspend fun open(): VaultResult<String>

    /** Borra el token cifrado y la clave. */
    suspend fun clear()
}

enum class DeviceTokenState {
    NONE,
    READY,

    /** Había un token, pero la clave ya no sirve (por ejemplo, se registró una huella nueva). Ya se borró. */
    INVALIDATED
}

/** Una huella ya autorizó guardar: [seal] cifra y guarda el token; [discard] lo abandona sin guardar nada. */
interface PendingSeal {
    suspend fun seal(deviceToken: String): Boolean

    suspend fun discard()
}

sealed interface VaultResult<out T> {
    data class Success<T>(val value: T) : VaultResult<T>

    /** Se cerró el diálogo sin una huella válida: no cambió nada. */
    data object Cancelled : VaultResult<Nothing>

    /** La clave ya no sirve: lo guardado se borró. */
    data object Invalidated : VaultResult<Nothing>

    /** No se pudo crear la clave (sin huella registrada) o mostrar el diálogo. */
    data object Unavailable : VaultResult<Nothing>
}
