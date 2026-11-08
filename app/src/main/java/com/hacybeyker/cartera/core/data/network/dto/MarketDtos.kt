package com.hacybeyker.cartera.core.data.network.dto

import kotlinx.serialization.Serializable

/** Un elemento de `GET /market/stocks` y el mensaje `quote` del canal en vivo (con `type`, que se ignora). */
@Serializable
data class StockQuoteDto(
    val symbol: String,
    val name: String,
    val currency: String,
    val last: String,
    val change: String,
    val changePct: String,
    val ts: String
)

@Serializable
data class StockListDto(val items: List<StockQuoteDto>)

@Serializable
data class StockDetailDto(
    val symbol: String,
    val name: String,
    val currency: String,
    val last: String,
    val change: String,
    val changePct: String,
    val ts: String,
    val open: String,
    val high: String,
    val low: String,
    val previousClose: String,
    val volume: Long,
    val description: String
)

@Serializable
data class PricePointDto(val ts: String, val price: String)

@Serializable
data class PriceHistoryDto(val symbol: String, val range: String, val currency: String, val points: List<PricePointDto>)

/** `GET /fx/rates` y el mensaje `fx` del canal en vivo. */
@Serializable
data class FxQuoteDto(
    val pair: String,
    val buy: String,
    val sell: String,
    val quoteId: String,
    val expiresAt: String,
    val ts: String
)

/** Mensajes `subscribe` y `unsubscribe` del canal en vivo. */
@Serializable
data class StreamSubscriptionDto(val type: String, val symbols: List<String>, val pairs: List<String>)
