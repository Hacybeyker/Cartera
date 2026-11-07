package com.hacybeyker.cartera.feature.terms.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.config.AppConfigRepository
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.domain.entry.ResolveEntryStepUseCase
import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import com.hacybeyker.cartera.core.domain.flatMap
import javax.inject.Inject

/** Guarda la versión vigente de los términos como aceptada y dice a qué pantalla seguir. */
class AcceptTermsUseCase @Inject constructor(
    private val appConfig: AppConfigRepository,
    private val firstRun: FirstRunRepository,
    private val resolveEntryStep: ResolveEntryStepUseCase
) {
    suspend operator fun invoke(): Outcome<EntryStep> = appConfig.config().flatMap { config ->
        firstRun.acceptTerms(config.termsVersion)
        resolveEntryStep()
    }
}
