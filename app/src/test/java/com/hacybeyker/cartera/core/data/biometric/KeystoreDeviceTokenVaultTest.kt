package com.hacybeyker.cartera.core.data.biometric

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.core.domain.biometric.DeviceTokenState
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** El Keystore y el diálogo se reemplazan por una clave AES de software y una huella que se decide en el test. */
@RunWith(AndroidJUnit4::class)
class KeystoreDeviceTokenVaultTest {
    private val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences(KeystoreDeviceTokenVault.FILE_NAME, Context.MODE_PRIVATE)
    private val ciphers = SoftwareCiphers()
    private val prompter = FakePrompter()

    @Test
    fun `nothing stored is none`() = runTest {
        assertEquals(DeviceTokenState.NONE, vault().state())
    }

    @Test
    fun `sealing stores only the encrypted token and its iv, and opening returns it`() = runTest {
        val vault = vault()
        val pending = (vault.unlockForSealing() as VaultResult.Success).value

        assertTrue(pending.seal("dvc_secret"))

        assertEquals(setOf("device_token_ciphertext", "device_token_iv"), preferences.all.keys)
        assertFalse(preferences.all.values.any { it.toString().contains("dvc_secret") })
        assertEquals(DeviceTokenState.READY, vault.state())
        assertEquals(VaultResult.Success("dvc_secret"), vault.open())
        assertEquals(listOf(PromptPurpose.ENROLL, PromptPurpose.LOG_IN), prompter.purposes)
    }

    @Test
    fun `a dismissed dialog while sealing leaves nothing stored and no key`() = runTest {
        prompter.authenticates = false

        assertEquals(VaultResult.Cancelled, vault().unlockForSealing())

        assertTrue(preferences.all.isEmpty())
        assertNull(ciphers.key)
    }

    @Test
    fun `a discarded seal leaves nothing stored and no key`() = runTest {
        val pending = (vault().unlockForSealing() as VaultResult.Success).value

        pending.discard()

        assertTrue(preferences.all.isEmpty())
        assertNull(ciphers.key)
    }

    @Test
    fun `without a key to create sealing is unavailable`() = runTest {
        ciphers.canCreate = false

        assertEquals(VaultResult.Unavailable, vault().unlockForSealing())
        assertEquals(emptyList<PromptPurpose>(), prompter.purposes)
    }

    @Test
    fun `a cipher that fails to encrypt does not store anything`() = runTest {
        val pending = (vault().unlockForSealing() as VaultResult.Success).value
        // Un cifrador que ya no corresponde a la clave: al cifrar falla la verificación del tag.
        prompter.used.single().init(Cipher.DECRYPT_MODE, KeyGenerator.getInstance("AES").generateKey(), gcm())

        assertFalse(pending.seal("dvc_secret"))

        assertTrue(preferences.all.isEmpty())
    }

    @Test
    fun `an invalidated key is reported once and wipes what was stored`() = runTest {
        val vault = sealed("dvc_secret")
        ciphers.invalidated = true

        assertEquals(DeviceTokenState.INVALIDATED, vault.state())

        assertTrue(preferences.all.isEmpty())
        assertNull(ciphers.key)
        assertEquals(DeviceTokenState.NONE, vault.state())
    }

    @Test
    fun `opening with an invalidated key wipes and does not show the dialog`() = runTest {
        val vault = sealed("dvc_secret")
        ciphers.invalidated = true

        assertEquals(VaultResult.Invalidated, vault.open())

        assertEquals(listOf(PromptPurpose.ENROLL), prompter.purposes)
        assertTrue(preferences.all.isEmpty())
    }

    @Test
    fun `opening with nothing stored is invalidated`() = runTest {
        assertEquals(VaultResult.Invalidated, vault().open())
    }

    @Test
    fun `a dismissed dialog while opening keeps the token`() = runTest {
        val vault = sealed("dvc_secret")
        prompter.authenticates = false

        assertEquals(VaultResult.Cancelled, vault.open())

        assertEquals(DeviceTokenState.READY, vault.state())
    }

    @Test
    fun `a ciphertext that does not authenticate is invalidated and wiped`() = runTest {
        val vault = sealed("dvc_secret")
        preferences.edit().putString("device_token_ciphertext", "AAAAAAAAAAAAAAAAAAAAAAAAAA==").commit()

        assertEquals(VaultResult.Invalidated, vault.open())

        assertTrue(preferences.all.isEmpty())
    }

    @Test
    fun `stored values that are not base64 count as nothing stored`() = runTest {
        preferences.edit().putString("device_token_ciphertext", "%%").putString("device_token_iv", "%%").commit()

        assertEquals(DeviceTokenState.NONE, vault().state())
    }

    @Test
    fun `clear wipes the token and the key`() = runTest {
        val vault = sealed("dvc_secret")

        vault.clear()

        assertTrue(preferences.all.isEmpty())
        assertNull(ciphers.key)
    }

    @Test
    fun `sealing again replaces the previous key and token`() = runTest {
        val vault = sealed("dvc_old")
        val oldKey = ciphers.key

        (vault.unlockForSealing() as VaultResult.Success).value.seal("dvc_new")

        assertNotNull(ciphers.key)
        assertFalse(oldKey === ciphers.key)
        assertEquals(VaultResult.Success("dvc_new"), vault.open())
    }

    private suspend fun TestScope.sealed(token: String): KeystoreDeviceTokenVault {
        val vault = vault()
        (vault.unlockForSealing() as VaultResult.Success).value.seal(token)
        return vault
    }

    private fun TestScope.vault() =
        KeystoreDeviceTokenVault(preferences, ciphers, prompter, StandardTestDispatcher(testScheduler))

    private fun gcm() = GCMParameterSpec(128, ByteArray(12))

    private class SoftwareCiphers : BiometricCiphers {
        var key: SecretKey? = null
        var canCreate = true
        var invalidated = false

        override fun newEncryptCipher(): Cipher? {
            if (!canCreate) return null
            val created = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            key = created
            return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, created) }
        }

        override fun decryptCipher(iv: ByteArray): Cipher? = key?.takeUnless { invalidated }?.let { current ->
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, current, GCMParameterSpec(128, iv))
            }
        }

        override fun deleteKey() {
            key = null
        }
    }

    private class FakePrompter : BiometricPrompter {
        var authenticates = true
        val purposes = mutableListOf<PromptPurpose>()
        val used = mutableListOf<Cipher>()

        override suspend fun authenticate(cipher: Cipher, purpose: PromptPurpose): PromptResult {
            purposes += purpose
            used += cipher
            return if (authenticates) PromptResult.Authenticated(cipher) else PromptResult.Dismissed
        }
    }
}
