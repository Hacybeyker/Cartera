package com.hacybeyker.cartera.core.data.biometric

import android.content.SharedPreferences
import androidx.core.content.edit
import com.hacybeyker.cartera.core.common.IoDispatcher
import com.hacybeyker.cartera.core.domain.biometric.DeviceTokenState
import com.hacybeyker.cartera.core.domain.biometric.DeviceTokenVault
import com.hacybeyker.cartera.core.domain.biometric.PendingSeal
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Solo se guardan el `deviceToken` cifrado y su IV; la clave vive en el Keystore y exige la huella en cada uso. */
class KeystoreDeviceTokenVault @Inject constructor(
    @BiometricStorage private val preferences: SharedPreferences,
    private val ciphers: BiometricCiphers,
    private val prompter: BiometricPrompter,
    @IoDispatcher private val io: CoroutineDispatcher
) : DeviceTokenVault {

    override suspend fun state(): DeviceTokenState = withContext(io) {
        val sealed = readSealed()
        when {
            sealed == null -> DeviceTokenState.NONE
            ciphers.decryptCipher(sealed.iv) != null -> DeviceTokenState.READY
            else -> {
                wipe()
                DeviceTokenState.INVALIDATED
            }
        }
    }

    override suspend fun unlockForSealing(): VaultResult<PendingSeal> {
        val cipher = withContext(io) {
            // La clave nueva deja inservible lo que hubiera guardado con la anterior.
            wipe()
            ciphers.newEncryptCipher()
        } ?: return VaultResult.Unavailable
        return when (val prompt = prompter.authenticate(cipher, PromptPurpose.ENROLL)) {
            is PromptResult.Authenticated -> VaultResult.Success(Sealer(prompt.cipher))
            PromptResult.Dismissed -> {
                withContext(io) { wipe() }
                VaultResult.Cancelled
            }
        }
    }

    override suspend fun open(): VaultResult<String> {
        val sealed = withContext(io) { readSealed() }
        val cipher = sealed?.let { withContext(io) { ciphers.decryptCipher(it.iv) } }
        if (sealed == null || cipher == null) return invalidated()
        return when (val prompt = prompter.authenticate(cipher, PromptPurpose.LOG_IN)) {
            is PromptResult.Authenticated -> decrypt(prompt.cipher, sealed.ciphertext)
            PromptResult.Dismissed -> VaultResult.Cancelled
        }
    }

    override suspend fun clear() = withContext(io) { wipe() }

    private suspend fun decrypt(cipher: Cipher, ciphertext: ByteArray): VaultResult<String> = withContext(io) {
        try {
            VaultResult.Success(cipher.doFinal(ciphertext).decodeToString())
        } catch (_: GeneralSecurityException) {
            wipe()
            VaultResult.Invalidated
        }
    }

    private suspend fun invalidated(): VaultResult<Nothing> {
        withContext(io) { wipe() }
        return VaultResult.Invalidated
    }

    private fun readSealed(): Sealed? {
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null)
        val iv = preferences.getString(KEY_IV, null)
        if (ciphertext == null || iv == null) return null
        return try {
            Sealed(ciphertext = decoder.decode(ciphertext), iv = decoder.decode(iv))
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun wipe() {
        preferences.edit(commit = true) { clear() }
        ciphers.deleteKey()
    }

    private inner class Sealer(private val cipher: Cipher) : PendingSeal {
        override suspend fun seal(deviceToken: String): Boolean = withContext(io) {
            try {
                val ciphertext = cipher.doFinal(deviceToken.encodeToByteArray())
                preferences.edit()
                    .putString(KEY_CIPHERTEXT, encoder.encodeToString(ciphertext))
                    .putString(KEY_IV, encoder.encodeToString(cipher.iv))
                    .commit()
            } catch (_: GeneralSecurityException) {
                wipe()
                false
            }
        }

        override suspend fun discard() = withContext(io) { wipe() }
    }

    private class Sealed(val ciphertext: ByteArray, val iv: ByteArray)

    companion object {
        const val FILE_NAME = "cartera_biometric"
        private const val KEY_CIPHERTEXT = "device_token_ciphertext"
        private const val KEY_IV = "device_token_iv"
        private val encoder = Base64.getEncoder()
        private val decoder = Base64.getDecoder()
    }
}
