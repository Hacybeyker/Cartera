package com.hacybeyker.cartera.feature.home.domain.usecase

import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import com.hacybeyker.cartera.feature.home.domain.model.CoachMark
import javax.inject.Inject

/** El primer paso de los coach marks si todavía no se resolvieron en esta instalación; si no, `null`. */
class GetPendingCoachMarkUseCase @Inject constructor(private val firstRun: FirstRunRepository) {
    suspend operator fun invoke(): CoachMark? = if (firstRun.isCoachMarksSeen()) null else CoachMark.first
}
