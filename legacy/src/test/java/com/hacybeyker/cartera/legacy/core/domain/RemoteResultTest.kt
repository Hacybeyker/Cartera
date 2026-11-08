package com.hacybeyker.cartera.legacy.core.domain

import io.reactivex.rxjava3.core.Single
import org.junit.Test

class RemoteResultTest {

    @Test
    fun `a value becomes a success`() {
        Single.just("ok").toResult().test().assertValue(RemoteResult.Success("ok"))
    }

    @Test
    fun `a translated error becomes a failure and the stream completes`() {
        Single.error<String>(LegacyException(LegacyError.Network))
            .toResult()
            .test()
            .assertValue(RemoteResult.Failure(LegacyError.Network))
            .assertComplete()
    }

    @Test
    fun `a programming error is not turned into a failure`() {
        val programmingError = IllegalStateException("programming error")

        Single.error<String>(programmingError).toResult().test().assertError(programmingError)
    }

    @Test
    fun `problem codes parse from the wire and unknown ones are kept as unknown`() {
        org.junit.Assert.assertEquals(ProblemCode.CUSTOMER_NOT_FOUND, ProblemCode.fromWire("CUSTOMER_NOT_FOUND"))
        org.junit.Assert.assertEquals(ProblemCode.UNKNOWN, ProblemCode.fromWire("CONFLICT"))
    }
}
