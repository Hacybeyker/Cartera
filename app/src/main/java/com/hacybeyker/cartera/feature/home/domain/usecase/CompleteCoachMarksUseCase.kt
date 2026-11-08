package com.hacybeyker.cartera.feature.home.domain.usecase

import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import javax.inject.Inject

/** Omitir o Entendido: los coach marks no vuelven a aparecer en esta instalación. */
class CompleteCoachMarksUseCase @Inject constructor(private val firstRun: FirstRunRepository) {
    suspend operator fun invoke() = firstRun.markCoachMarksSeen()
}
