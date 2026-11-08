package com.hacybeyker.cartera.core.data.account

import com.hacybeyker.cartera.core.domain.account.AccountChanges
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryAccountChanges @Inject constructor() : AccountChanges {
    private val counter = AtomicInteger()

    override val revision: Int get() = counter.get()

    override fun notifyChanged() {
        counter.incrementAndGet()
    }
}
