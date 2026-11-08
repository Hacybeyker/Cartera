package com.hacybeyker.cartera.core.domain.firstrun

/**
 * Lo que el dispositivo recuerda entre sesiones del mismo usuario: términos aceptados, onboarding visto, la oferta de
 * ingreso con huella resuelta y los coach marks de Home vistos.
 */
interface FirstRunRepository {
    suspend fun acceptedTermsVersion(): String?

    suspend fun acceptTerms(version: String)

    suspend fun isOnboardingSeen(): Boolean

    suspend fun markOnboardingSeen()

    suspend fun isBiometricOfferResolved(): Boolean

    suspend fun markBiometricOfferResolved()

    suspend fun isCoachMarksSeen(): Boolean

    suspend fun markCoachMarksSeen()
}
