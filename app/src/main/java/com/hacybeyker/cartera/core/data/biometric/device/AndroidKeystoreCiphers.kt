package com.hacybeyker.cartera.core.data.biometric.device

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.hacybeyker.cartera.core.data.biometric.BiometricCiphers
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/** AES-256-GCM en el Android Keystore, con una clave que solo una huella (biometría fuerte) autoriza en cada uso. */
class AndroidKeystoreCiphers @Inject constructor() : BiometricCiphers {

    override fun newEncryptCipher(): Cipher? = try {
        deleteKey()
        cipher().apply { init(Cipher.ENCRYPT_MODE, generateKey()) }
    } catch (_: GeneralSecurityException) {
        // Sin una huella registrada, el Keystore no crea la clave.
        deleteKey()
        null
    } catch (_: ProviderException) {
        deleteKey()
        null
    }

    override fun decryptCipher(iv: ByteArray): Cipher? = try {
        (keyStore().getKey(ALIAS, null) as? SecretKey)?.let { key ->
            cipher().apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv)) }
        }
    } catch (_: GeneralSecurityException) {
        // KeyPermanentlyInvalidatedException: se registró una huella nueva o se quitó el bloqueo de pantalla.
        null
    }

    override fun deleteKey() {
        try {
            keyStore().deleteEntry(ALIAS)
        } catch (_: GeneralSecurityException) {
            // Si el Keystore no responde, no hay una clave que borrar que se pueda usar.
        }
    }

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                // Antes de la API 30, una clave sin tiempo de validez ya exige una huella en cada uso.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                }
            }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun cipher(): Cipher = Cipher.getInstance(TRANSFORMATION)

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "cartera_device_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
    }
}
