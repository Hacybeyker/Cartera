package com.hacybeyker.cartera.core.data.portfolio

import com.hacybeyker.cartera.core.domain.portfolio.PortfolioChanges
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryPortfolioChanges @Inject constructor() : PortfolioChanges {
    private val counter = AtomicInteger()

    override val revision: Int get() = counter.get()

    override fun notifyChanged() {
        counter.incrementAndGet()
    }
}
