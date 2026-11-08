package com.hacybeyker.cartera.core.domain.portfolio

import javax.inject.Inject

class GetPortfolioRevisionUseCase @Inject constructor(private val changes: PortfolioChanges) {
    operator fun invoke(): Int = changes.revision
}
