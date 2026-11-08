package com.hacybeyker.cartera.core.data.biometric

import javax.crypto.Cipher

/** La clave que protege el `deviceToken` y los cifradores que la huella autoriza. */
interface BiometricCiphers {
    /** Reemplaza la clave por una nueva y devuelve un cifrador para cifrar; `null` si no se pudo crear. */
    fun newEncryptCipher(): Cipher?

    /** Cifrador para descifrar lo guardado con [iv]; `null` si la clave no existe o quedó invalidada. */
    fun decryptCipher(iv: ByteArray): Cipher?

    fun deleteKey()
}
