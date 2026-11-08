package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class StockFixture(
    val symbol: String,
    val name: String,
    val currency: String,
    val previousClose: BigDecimal,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val volume: Long,
    val description: String
) {
    companion object {
        fun from(json: JsonObject) = StockFixture(
            symbol = json.string("symbol"),
            name = json.string("name"),
            currency = json.string("currency"),
            previousClose = json.decimal("previousClose"),
            open = json.decimal("open"),
            high = json.decimal("high"),
            low = json.decimal("low"),
            volume = json.getValue("volume").jsonPrimitive.long,
            description = json.string("description")
        )
    }
}

enum class HistoryRange(val wireName: String) {
    ONE_DAY("1D"),
    ONE_WEEK("1W"),
    ONE_MONTH("1M"),
    ONE_YEAR("1Y");

    companion object {
        fun fromWire(value: String?): HistoryRange? = entries.firstOrNull { it.wireName == value }
    }
}

/** Precios equiespaciados que terminan en el precio inicial; el backend les pone fecha al responder. */
data class PriceSeries(val stepSeconds: Long, val prices: List<BigDecimal>) {
    companion object {
        fun from(json: JsonObject) = PriceSeries(
            stepSeconds = json.getValue("stepSeconds").jsonPrimitive.long,
            prices = json.getValue("prices").jsonArray.map { BigDecimal(it.jsonPrimitive.content) }
        )
    }
}

data class FxTick(val buy: BigDecimal, val sell: BigDecimal) {
    companion object {
        fun from(json: JsonObject) = FxTick(buy = json.decimal("buy"), sell = json.decimal("sell"))
    }
}

data class Holding(val symbol: String, val quantity: Int, val averagePrice: BigDecimal)

data class PortfolioFixture(val userId: String, val accountId: String, val holdings: List<Holding>) {
    companion object {
        fun from(json: JsonObject) = PortfolioFixture(
            userId = json.string("userId"),
            accountId = json.string("accountId"),
            holdings = json.getValue("holdings").jsonArray.map { element ->
                val holding = element.jsonObject
                Holding(
                    symbol = holding.string("symbol"),
                    quantity = holding.getValue("quantity").jsonPrimitive.int,
                    averagePrice = holding.decimal("averagePrice")
                )
            }
        )
    }
}

fun JsonObject.decimal(key: String): BigDecimal = BigDecimal(string(key))
