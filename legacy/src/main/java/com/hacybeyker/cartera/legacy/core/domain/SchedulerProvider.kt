package com.hacybeyker.cartera.legacy.core.domain

import io.reactivex.rxjava3.core.Scheduler

interface SchedulerProvider {
    fun io(): Scheduler

    fun computation(): Scheduler

    fun main(): Scheduler
}
