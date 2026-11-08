package com.hacybeyker.cartera.demo.backend

import java.io.Closeable
import java.net.InetAddress
import java.time.Clock
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.HttpUrl

sealed interface Outcome {
    data class Respond(val response: BackendResponse) : Outcome
    data object Stall : Outcome
    data object Disconnect : Outcome
    data class Upgrade(val handshake: StreamHandshake) : Outcome
}

data class Handled(val outcome: Outcome, val latencyMs: Long)

class CarteraBackend(
    fixtures: FixtureSource,
    clock: Clock = Clock.systemDefaultZone(),
    devices: DeviceRegistry = InMemoryDeviceRegistry(),
    private val log: (String) -> Unit = {}
) : Closeable {
    private val state = BackendState(FixtureCatalog(fixtures), clock, devices)
    private val failures = FailureInjector()
    private val desks = BackendDesks.create(state, JwtSigner(clock), clock)
    private val requestCounter = AtomicLong()
    private val server = MockWebServer()

    // Un solo hilo: el estado no necesita locks y el trabajo del backend aparece con nombre propio en las trazas.
    // Los ticks y temporizadores del canal en vivo corren en ese mismo hilo.
    private val worker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, WORKER_THREAD).apply { isDaemon = true }
    }
    private val stream = MarketStream(
        state = state,
        desks = desks,
        clock = clock,
        scheduler = { delayMs, task ->
            val future = worker.schedule(Runnable(task), delayMs, TimeUnit.MILLISECONDS)
            Cancellable { future.cancel(false) }
        },
        log = log
    )
    private val router = BackendRouter(
        state = state,
        desks = desks,
        onReset = {
            failures.reset()
            stream.reset()
        }
    )

    val baseUrl: HttpUrl get() = server.url(Endpoint.BASE_PATH)

    fun start(address: InetAddress = InetAddress.getByName(LOOPBACK), port: Int = PORT) {
        server.dispatcher = BackendDispatcher()
        server.start(address, port)
    }

    fun handle(request: BackendRequest): Handled {
        val endpoint = Endpoint.resolve(request.method, request.path)?.endpoint
        if (endpoint == null || endpoint == Endpoint.DEMO_RESET) {
            return Handled(Outcome.Respond(router.route(request)), latencyMs = 0)
        }
        val scenario = DemoScenario.from(request)
        val outcome = when {
            scenario.backendDown -> Outcome.Respond(
                problem(
                    code = ProblemCode.SERVICE_UNAVAILABLE,
                    detail = "El servicio no está disponible.",
                    traceId = request.requestId,
                    headers = mapOf("Retry-After" to RETRY_AFTER_SECONDS)
                )
            )
            failures.shouldFail(scenario.failureRate) -> injectedFailure(scenario.failureMode, request.requestId)
            else -> routed(request)
        }
        return Handled(outcome, scenario.latencyMs)
    }

    private fun routed(request: BackendRequest): Outcome {
        val response = router.route(request)
        return if (response.status == BackendRouter.SWITCHING_PROTOCOLS) {
            Outcome.Upgrade(stream.handshake(request))
        } else {
            Outcome.Respond(response)
        }
    }

    override fun close() {
        server.close()
        worker.shutdownNow()
    }

    private fun injectedFailure(mode: FailureMode, traceId: String): Outcome = when (mode) {
        FailureMode.HTTP_500 -> Outcome.Respond(problem(ProblemCode.INTERNAL_ERROR, "Fallo inyectado.", traceId))
        FailureMode.TIMEOUT -> Outcome.Stall
        FailureMode.DISCONNECT -> Outcome.Disconnect
    }

    private inner class BackendDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val startedAt = System.nanoTime()
            val backendRequest = request.toBackendRequest()
            val handled = worker.submit(Callable { handle(backendRequest) }).get()
            val builder = MockResponse.Builder()
                .headersDelay(handled.latencyMs, TimeUnit.MILLISECONDS)
                .addHeader(REQUEST_ID_HEADER, backendRequest.requestId)
            val status = when (val outcome = handled.outcome) {
                is Outcome.Respond -> {
                    builder.respond(outcome.response)
                    outcome.response.status.toString()
                }
                Outcome.Stall -> {
                    builder.onResponseStart(SocketEffect.Stall)
                    "timeout"
                }
                Outcome.Disconnect -> {
                    builder.onResponseStart(SocketEffect.CloseSocket())
                    "disconnect"
                }
                is Outcome.Upgrade -> {
                    builder.webSocketUpgrade(StreamListener(outcome.handshake, stream, ::post))
                    "101 WS#${outcome.handshake.id}"
                }
            }
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt) + handled.latencyMs
            log(
                "${request.method} ${request.target} $status ${elapsedMs}ms " +
                    "latency=${handled.latencyMs}ms $REQUEST_ID_HEADER=${backendRequest.requestId}"
            )
            return builder.build()
        }
    }

    private fun post(action: () -> Unit) {
        try {
            worker.execute(Runnable(action))
        } catch (_: RejectedExecutionException) {
            // El backend se está cerrando: los eventos de los sockets que se cierran con él ya no importan.
        }
    }

    private fun MockResponse.Builder.respond(response: BackendResponse) {
        code(response.status)
        response.headers.forEach { (name, value) -> addHeader(name, value) }
        response.body?.let {
            addHeader("Content-Type", "${response.contentType}; charset=utf-8")
            body(it)
        }
    }

    private fun RecordedRequest.toBackendRequest(): BackendRequest = BackendRequest(
        method = method,
        path = url.encodedPath,
        query = url.queryParameterNames.associateWith { url.queryParameter(it).orEmpty() },
        headers = headers.toMap(),
        body = body?.utf8().orEmpty(),
        requestId = headers[REQUEST_ID_HEADER] ?: "req-%06d".format(requestCounter.incrementAndGet())
    )

    companion object {
        const val PORT = 8765
        const val LOOPBACK = "127.0.0.1"
        const val WORKER_THREAD = "CarteraBackend-worker"
        const val REQUEST_ID_HEADER = "X-Request-Id"
        private const val RETRY_AFTER_SECONDS = "30"
    }
}
