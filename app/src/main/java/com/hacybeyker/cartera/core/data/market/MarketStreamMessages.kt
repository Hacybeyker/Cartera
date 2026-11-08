package com.hacybeyker.cartera.core.data.market

import com.hacybeyker.cartera.core.data.network.dto.FxQuoteDto
import com.hacybeyker.cartera.core.data.network.dto.StockQuoteDto
import com.hacybeyker.cartera.core.data.network.dto.StreamSubscriptionDto
import com.hacybeyker.cartera.core.data.network.dto.toFxQuote
import com.hacybeyker.cartera.core.data.network.dto.toQuote
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.core.domain.market.Quote
import java.time.DateTimeException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Lo que llega del servidor por el canal en vivo. */
internal sealed interface StreamMessage {
    data class Price(val quote: Quote) : StreamMessage

    data class Fx(val quote: FxQuote) : StreamMessage
}

/** Mensajes del canal (`api/asyncapi.yaml`): texto JSON con un campo `type`. */
internal class MarketStreamMessages(private val json: Json) {

    // El protocolo pide ignorar lo que no se entiende, sin cerrar: un mensaje inválido no tumba el canal.
    fun parse(text: String): StreamMessage? = try {
        val message = json.parseToJsonElement(text).jsonObject
        when (message["type"]?.jsonPrimitive?.contentOrNull) {
            QUOTE -> StreamMessage.Price(json.decodeFromJsonElement(StockQuoteDto.serializer(), message).toQuote())
            FX -> StreamMessage.Fx(json.decodeFromJsonElement(FxQuoteDto.serializer(), message).toFxQuote())
            else -> null
        }
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: DateTimeException) {
        null
    }

    /** `subscribe` o `unsubscribe`; `null` si no nombra nada, porque no hace falta enviarlo. */
    fun subscription(subscribe: Boolean, symbols: Collection<String>, pairs: Collection<String>): String? {
        if (symbols.isEmpty() && pairs.isEmpty()) return null
        val type = if (subscribe) SUBSCRIBE else UNSUBSCRIBE
        return json.encodeToString(
            StreamSubscriptionDto.serializer(),
            StreamSubscriptionDto(type, symbols.toList(), pairs.toList())
        )
    }

    private companion object {
        const val SUBSCRIBE = "subscribe"
        const val UNSUBSCRIBE = "unsubscribe"
        const val QUOTE = "quote"
        const val FX = "fx"
    }
}
