package com.hacybeyker.cartera.legacy.testing

import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import com.hacybeyker.cartera.legacy.debts.data.DebtsBodyDecoder
import com.hacybeyker.cartera.legacy.debts.domain.CustomerDebts
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSource

const val IO_THREAD = "test-io"
const val MAIN_THREAD = "test-main"

/** `io` y `main` son hilos con nombre, para ver en cuál corre cada paso. */
class NamedThreadSchedulers :
    SchedulerProvider,
    AutoCloseable {
    private val ioExecutor = singleThread(IO_THREAD)
    private val mainExecutor = singleThread(MAIN_THREAD)
    private val io = Schedulers.from(ioExecutor)
    private val main = Schedulers.from(mainExecutor)

    override fun io(): Scheduler = io

    override fun computation(): Scheduler = Schedulers.computation()

    override fun main(): Scheduler = main

    override fun close() {
        ioExecutor.shutdownNow()
        mainExecutor.shutdownNow()
    }

    private fun singleThread(name: String): ExecutorService = Executors.newSingleThreadExecutor { Thread(it, name) }
}

/** Anota el hilo de cada decodificación y delega en el decodificador real. */
class RecordingDebtsBodyDecoder(private val delegate: DebtsBodyDecoder) : DebtsBodyDecoder {
    val threads = CopyOnWriteArrayList<String>()

    override fun decode(body: String): CustomerDebts {
        threads += Thread.currentThread().name
        return delegate.decode(body)
    }
}

/** Anota el hilo de cada llamada y el de cada lectura del cuerpo. */
class ThreadRecordingInterceptor : Interceptor {
    val callThreads = CopyOnWriteArrayList<String>()
    val bodyThreads = CopyOnWriteArrayList<String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        callThreads += Thread.currentThread().name
        val response = chain.proceed(chain.request())
        val body = checkNotNull(response.body)
        return response.newBuilder().body(RecordingBody(body, bodyThreads)).build()
    }

    private class RecordingBody(private val delegate: ResponseBody, private val threads: MutableList<String>) :
        ResponseBody() {
        override fun contentType(): MediaType? = delegate.contentType()

        override fun contentLength(): Long = delegate.contentLength()

        override fun source(): BufferedSource {
            threads += Thread.currentThread().name
            return delegate.source()
        }
    }
}
