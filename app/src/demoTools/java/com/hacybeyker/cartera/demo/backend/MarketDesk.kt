package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Mercado y portafolio. Todo se calcula con el precio vigente del servidor ([MarketState]). */
class MarketDesk(private val state: BackendState, private val clock: Clock) {

    fun stocks(): BackendResponse = json(
        OK,
        buildJsonObject { put("items", JsonArray(state.market.symbols.map { quoteJson(it) })) }
    )

    fun stock(symbol: String): BackendResponse {
        val quote = state.market.quote(symbol) ?: unknownSymbol(symbol)
        return json(
            OK,
            JsonObject(
                quoteJson(symbol) + buildJsonObject {
                    put("open", quote.stock.open.toPlainString())
                    put("high", quote.high.toPlainString())
                    put("low", quote.low.toPlainString())
                    put("previousClose", quote.stock.previousClose.toPlainString())
                    put("volume", quote.stock.volume)
                    put("description", quote.stock.description)
                }
            )
        )
    }

    fun history(symbol: String, query: Map<String, String>): BackendResponse {
        val stock = state.market.stock(symbol) ?: unknownSymbol(symbol)
        val range = HistoryRange.fromWire(query["range"])
            ?: throw validationProblem("range", "INVALID_VALUE", "Usa 1D, 1W, 1M o 1Y.")
        val series = state.market.history(symbol, range)
        val now = clock.instant().truncatedTo(ChronoUnit.SECONDS)
        val last = series.prices.lastIndex
        return json(
            OK,
            buildJsonObject {
                put("symbol", stock.symbol)
                put("range", range.wireName)
                put("currency", stock.currency)
                putJsonArray("points") {
                    series.prices.forEachIndexed { index, price ->
                        addJsonObject {
                            put("ts", timestamp(now.minusSeconds((last - index) * series.stepSeconds)))
                            put("price", price.toPlainString())
                        }
                    }
                }
            }
        )
    }

    /** Lo que publica el canal en vivo como `quote`, y cada elemento de `GET /market/stocks`. */
    fun quoteJson(symbol: String): JsonObject {
        val quote = requireNotNull(state.market.quote(symbol)) { "Símbolo desconocido: $symbol" }
        val change = quote.last - quote.stock.previousClose
        return buildJsonObject {
            put("symbol", quote.stock.symbol)
            put("name", quote.stock.name)
            put("currency", quote.stock.currency)
            put("last", quote.last.toPlainString())
            put("change", change.toPlainString())
            put("changePct", percent(change, quote.stock.previousClose))
            put("ts", timestamp(quote.tickedAt))
        }
    }

    fun portfolio(userId: String): BackendResponse {
        val positions = state.holdings.of(userId).map(::position)
        val marketValue = positions.sumOf { it.marketValue }
        val cost = positions.sumOf { it.cost }
        val dayChange = positions.sumOf { it.dayChange }
        return json(
            OK,
            buildJsonObject {
                put("accountId", state.holdings.settlementAccountId)
                put("currency", CURRENCY)
                putJsonArray("holdings") { positions.forEach { add(it.toJson()) } }
                putJsonObject("totals") {
                    put("marketValue", money(marketValue, CURRENCY))
                    put("cost", money(cost, CURRENCY))
                    put("gain", money(marketValue - cost, CURRENCY))
                    put("gainPct", percent(marketValue - cost, cost))
                    put("dayChange", money(dayChange, CURRENCY))
                    put("dayChangePct", percent(dayChange, marketValue - dayChange))
                }
            }
        )
    }

    private fun position(holding: Holding): Position {
        val quote = requireNotNull(state.market.quote(holding.symbol))
        val quantity = holding.quantity.toBigDecimal()
        return Position(
            holding = holding,
            name = quote.stock.name,
            lastPrice = quote.last,
            marketValue = cents(quote.last * quantity),
            cost = cents(holding.averagePrice * quantity),
            dayChange = cents((quote.last - quote.stock.previousClose) * quantity)
        )
    }

    private data class Position(
        val holding: Holding,
        val name: String,
        val lastPrice: BigDecimal,
        val marketValue: BigDecimal,
        val cost: BigDecimal,
        val dayChange: BigDecimal
    ) {
        fun toJson(): JsonObject = buildJsonObject {
            put("symbol", holding.symbol)
            put("name", name)
            put("quantity", holding.quantity)
            put("averagePrice", money(holding.averagePrice, CURRENCY))
            put("lastPrice", money(lastPrice, CURRENCY))
            put("marketValue", money(marketValue, CURRENCY))
            put("cost", money(cost, CURRENCY))
            put("gain", money(marketValue - cost, CURRENCY))
            put("gainPct", percent(marketValue - cost, cost))
            put("dayChange", money(dayChange, CURRENCY))
        }
    }

    private fun unknownSymbol(symbol: String): Nothing =
        throw ProblemException(ProblemCode.NOT_FOUND, "No se encontró la acción $symbol.")

    companion object {
        const val CURRENCY = "USD"
        private const val OK = 200
        private val HUNDRED = BigDecimal(100)

        /** Porcentaje con 2 decimales; "0.00" si la base es cero. */
        fun percent(part: BigDecimal, base: BigDecimal): String = if (base.signum() == 0) {
            "0.00"
        } else {
            part.multiply(HUNDRED).divide(base, 2, RoundingMode.HALF_UP).toPlainString()
        }
    }
}
