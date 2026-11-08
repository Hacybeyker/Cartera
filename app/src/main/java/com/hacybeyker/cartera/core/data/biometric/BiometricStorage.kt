package com.hacybeyker.cartera.core.data.biometric

import javax.inject.Qualifier

/** Las preferencias donde vive el `deviceToken` cifrado, fuera de los backups. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BiometricStorage
