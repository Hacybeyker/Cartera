package com.hacybeyker.cartera.legacy.core.data

import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class RxSchedulerProvider @Inject constructor() : SchedulerProvider {
    override fun io(): Scheduler = Schedulers.io()

    override fun computation(): Scheduler = Schedulers.computation()

    override fun main(): Scheduler = AndroidSchedulers.mainThread()
}
