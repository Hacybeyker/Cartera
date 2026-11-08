package com.hacybeyker.cartera.core.domain.entry

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.biometric.ShouldOfferBiometricsUseCase
import com.hacybeyker.cartera.core.domain.config.AppConfigRepository
import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import com.hacybeyker.cartera.core.domain.map
import javax.inject.Inject

/**
 * Qué pantalla sigue tras el login: Términos si cambió su versión, Onboarding si no se vio, la oferta de ingreso con
 * huella si corresponde, si no Home.
 */
class ResolveEntryStepUseCase @Inject constructor(
    private val appConfig: AppConfigRepository,
    private val firstRun: FirstRunRepository,
    private val shouldOfferBiometrics: ShouldOfferBiometricsUseCase
) {
    suspend operator fun invoke(): Outcome<EntryStep> = appConfig.config().map { config -> next(config.termsVersion) }

    /** Lo que sigue al onboarding, que no depende de la configuración. */
    suspend fun afterOnboarding(): EntryStep =
        if (shouldOfferBiometrics()) EntryStep.BIOMETRIC_OFFER else EntryStep.HOME

    private suspend fun next(termsVersion: String): EntryStep = when {
        firstRun.acceptedTermsVersion() != termsVersion -> EntryStep.TERMS
        !firstRun.isOnboardingSeen() -> EntryStep.ONBOARDING
        else -> afterOnboarding()
    }
}
