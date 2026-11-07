package com.hacybeyker.cartera.core.domain.entry

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.config.AppConfigRepository
import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import com.hacybeyker.cartera.core.domain.map
import javax.inject.Inject

/** Qué pantalla sigue tras el login: Términos si cambió su versión, Onboarding si no se vio, si no Home. */
class ResolveEntryStepUseCase @Inject constructor(
    private val appConfig: AppConfigRepository,
    private val firstRun: FirstRunRepository
) {
    suspend operator fun invoke(): Outcome<EntryStep> = appConfig.config().map { config -> next(config.termsVersion) }

    private suspend fun next(termsVersion: String): EntryStep = when {
        firstRun.acceptedTermsVersion() != termsVersion -> EntryStep.TERMS
        !firstRun.isOnboardingSeen() -> EntryStep.ONBOARDING
        else -> EntryStep.HOME
    }
}
