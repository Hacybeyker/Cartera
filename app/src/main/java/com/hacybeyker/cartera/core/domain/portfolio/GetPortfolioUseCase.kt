package com.hacybeyker.cartera.core.domain.portfolio

import com.hacybeyker.cartera.core.domain.Outcome
import javax.inject.Inject

class GetPortfolioUseCase @Inject constructor(private val portfolio: PortfolioRepository) {
    suspend operator fun invoke(): Outcome<Portfolio> = portfolio.portfolio()
}
