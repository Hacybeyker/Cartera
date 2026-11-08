package com.hacybeyker.cartera.core.data.biometric

import javax.crypto.Cipher

/** Muestra el diálogo de huella del sistema para autorizar [Cipher]. */
interface BiometricPrompter {
    suspend fun authenticate(cipher: Cipher, purpose: PromptPurpose): PromptResult
}

enum class PromptPurpose {
    ENROLL,
    LOG_IN
}

sealed interface PromptResult {
    /** [cipher] quedó autorizado para una operación. */
    data class Authenticated(val cipher: Cipher) : PromptResult

    /** Se canceló, se bloqueó por intentos o no se pudo mostrar. */
    data object Dismissed : PromptResult
}
