package com.hacybeyker.cartera.feature.onboarding.domain.usecase

import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(private val firstRun: FirstRunRepository) {
    suspend operator fun invoke() = firstRun.markOnboardingSeen()
}
