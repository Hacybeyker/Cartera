package com.hacybeyker.cartera.core.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.TestCoroutineScheduler

/** Reloj que avanza con el tiempo virtual de [scheduler]: `delay` y `advanceTimeBy` también mueven la hora. */
class TestClock(private val scheduler: TestCoroutineScheduler, private val start: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = start.plusMillis(scheduler.currentTime)
}
