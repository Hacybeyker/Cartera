package com.hacybeyker.cartera.core.domain.firstrun

/**
 * Lo que el dispositivo recuerda entre sesiones del mismo usuario: términos aceptados, onboarding visto y la oferta de
 * ingreso con huella resuelta.
 */
interface FirstRunRepository {
    suspend fun acceptedTermsVersion(): String?

    suspend fun acceptTerms(version: String)

    suspend fun isOnboardingSeen(): Boolean

    suspend fun markOnboardingSeen()

    suspend fun isBiometricOfferResolved(): Boolean

    suspend fun markBiometricOfferResolved()
}
