package com.hacybeyker.cartera.core.domain.firstrun

/** Lo que el dispositivo recuerda entre instalaciones del mismo usuario: términos aceptados y onboarding visto. */
interface FirstRunRepository {
    suspend fun acceptedTermsVersion(): String?

    suspend fun acceptTerms(version: String)

    suspend fun isOnboardingSeen(): Boolean

    suspend fun markOnboardingSeen()
}
