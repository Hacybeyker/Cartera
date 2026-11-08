package com.hacybeyker.cartera.feature.onboarding.domain.usecase

import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.domain.entry.ResolveEntryStepUseCase
import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import javax.inject.Inject

/** Marca el onboarding como visto y dice a qué pantalla seguir: la oferta de ingreso con huella o Home. */
class CompleteOnboardingUseCase @Inject constructor(
    private val firstRun: FirstRunRepository,
    private val resolveEntryStep: ResolveEntryStepUseCase
) {
    suspend operator fun invoke(): EntryStep {
        firstRun.markOnboardingSeen()
        return resolveEntryStep.afterOnboarding()
    }
}
