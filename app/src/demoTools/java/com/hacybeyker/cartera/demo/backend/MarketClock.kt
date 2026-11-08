package com.hacybeyker.cartera.demo.backend

/**
 * Reloj de mercado global del canal en vivo: uno solo para todas las conexiones, así dos conexiones no aceleran
 * los precios. Corre al intervalo del último handshake; con `tick_ms` 0 queda detenido.
 */
class MarketClock(
    private val scheduler: StreamScheduler,
    private val log: (String) -> Unit,
    private val onTick: (tick: Int) -> Unit
) {
    private var tickMs = DemoScenario.DEFAULT_TICK_MS
    private var task: Cancellable? = null
    private var ticks = 0

    /** Fija el intervalo de un handshake nuevo. Si cambia, el próximo tick se cuenta desde ahora. */
    fun retime(newTickMs: Int) {
        val changed = newTickMs != tickMs
        tickMs = newTickMs
        if (tickMs == 0) {
            stop()
        } else if (task == null || changed) {
            task?.cancel()
            log("WS reloj cada $tickMs ms")
            schedule()
        }
    }

    fun stop() {
        task?.let {
            it.cancel()
            task = null
            log("WS reloj detenido")
        }
    }

    private fun schedule() {
        task = scheduler.schedule(tickMs.toLong()) {
            onTick(++ticks)
            if (task != null) schedule()
        }
    }
}
