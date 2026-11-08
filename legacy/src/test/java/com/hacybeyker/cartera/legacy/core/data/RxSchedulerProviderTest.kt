package com.hacybeyker.cartera.legacy.core.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RxSchedulerProviderTest {
    private val provider = RxSchedulerProvider()

    @Test
    fun `io and computation are the rx pools and main is the android main thread`() {
        assertSame(Schedulers.io(), provider.io())
        assertSame(Schedulers.computation(), provider.computation())
        assertSame(AndroidSchedulers.mainThread(), provider.main())
    }
}
