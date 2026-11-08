package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

data class StockQuote(
    val stock: StockFixture,
    val last: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val tickedAt: Instant
)

data class FxRate(val tick: FxTick, val tickedAt: Instant)

/**
 * Precio vigente de cada acción y del tipo de cambio: un índice sobre los ticks de los fixtures. Empieza en el
 * primero (el precio inicial) y solo avanza con [advance] o [advanceFx]; al agotarse vuelve al primero.
 * Como el resto del estado, se usa solo desde el hilo del backend.
 */
class MarketState(private val catalog: FixtureCatalog, private val clock: Clock) {
    private val tickIndex = mutableMapOf<String, Int>()
    private val tickedAt = mutableMapOf<String, Instant>()
    private val dayHigh = mutableMapOf<String, BigDecimal>()
    private val dayLow = mutableMapOf<String, BigDecimal>()
    private var fxIndex = 0
    private var fxTickedAt: Instant? = null
    private var openedAt: Instant = clock.instant()

    val symbols: List<String> get() = catalog.stocks.map { it.symbol }

    fun stock(symbol: String): StockFixture? = catalog.stocks.firstOrNull { it.symbol == symbol }

    fun price(symbol: String): BigDecimal = catalog.ticks(symbol)[tickIndex[symbol] ?: 0]

    fun quote(symbol: String): StockQuote? = stock(symbol)?.let { stock ->
        val last = price(symbol)
        StockQuote(
            stock = stock,
            last = last,
            high = maxOf(dayHigh[symbol] ?: stock.high, last),
            low = minOf(dayLow[symbol] ?: stock.low, last),
            tickedAt = tickedAt[symbol] ?: openedAt
        )
    }

    fun history(symbol: String, range: HistoryRange): PriceSeries = catalog.history(symbol).getValue(range)

    /** Pasa al siguiente tick de [symbol] y devuelve su cotización. */
    fun advance(symbol: String): StockQuote {
        val current = requireNotNull(quote(symbol)) { "Símbolo desconocido: $symbol" }
        dayHigh[symbol] = current.high
        dayLow[symbol] = current.low
        tickIndex[symbol] = ((tickIndex[symbol] ?: 0) + 1) % catalog.ticks(symbol).size
        tickedAt[symbol] = clock.instant()
        return requireNotNull(quote(symbol))
    }

    fun fx(): FxRate = FxRate(catalog.fxTicks[fxIndex], fxTickedAt ?: openedAt)

    fun advanceFx(): FxRate {
        fxIndex = (fxIndex + 1) % catalog.fxTicks.size
        fxTickedAt = clock.instant()
        return fx()
    }

    fun reset() {
        tickIndex.clear()
        tickedAt.clear()
        dayHigh.clear()
        dayLow.clear()
        fxIndex = 0
        fxTickedAt = null
        openedAt = clock.instant()
    }
}

/** Tenencias por usuario; empiezan en `portfolio.json`. */
class HoldingsBook(private val catalog: FixtureCatalog) {
    private var changed: MutableMap<String, MutableMap<String, Holding>>? = null

    val settlementAccountId: String get() = catalog.portfolio.accountId

    fun of(userId: String): List<Holding> = book()[userId].orEmpty().values.sortedBy { it.symbol }

    fun holding(userId: String, symbol: String): Holding? = book()[userId]?.get(symbol)

    /** Una tenencia en cero sale del portafolio. */
    fun put(userId: String, holding: Holding) {
        val holdings = book().getOrPut(userId) { mutableMapOf() }
        if (holding.quantity > 0) holdings[holding.symbol] = holding else holdings.remove(holding.symbol)
    }

    fun reset() {
        changed = null
    }

    // Se copia del fixture la primera vez que se usa: así el portafolio no se lee al arrancar la app.
    private fun book(): MutableMap<String, MutableMap<String, Holding>> = changed ?: mutableMapOf(
        catalog.portfolio.userId to catalog.portfolio.holdings.associateBy { it.symbol }.toMutableMap()
    ).also { changed = it }
}
