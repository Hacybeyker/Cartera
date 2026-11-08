package com.hacybeyker.cartera.feature.exchange.ui

import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.feature.exchange.ReferenceExchange
import com.hacybeyker.cartera.feature.exchange.ReferenceExchangeReceipt
import com.hacybeyker.cartera.feature.exchange.pen
import com.hacybeyker.cartera.feature.exchange.usd
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class FxNavKeysTest {
    @Test
    fun `the exchange survives the trip through its nav key, quote and idempotency key included`() {
        listOf(ReferenceExchange, ReferenceExchange.copy(direction = ExchangeDirection.PEN_TO_USD)).forEach { order ->
            val key = FxConfirmNavKey(order.toArgs())

            val restored = Json.decodeFromString(FxConfirmNavKey.serializer(), Json.encodeToString(key))

            val expected = if (order.direction == ExchangeDirection.USD_TO_PEN) {
                order
            } else {
                // Las monedas de las cuentas y del monto salen del sentido.
                order.copy(
                    source = order.source.copy(currency = "PEN"),
                    destination = order.destination.copy(currency = "USD"),
                    amount = pen("100.00")
                )
            }
            assertEquals(expected, restored.order.toOrder())
        }
    }

    @Test
    fun `the receipt keeps what the result shows, with the names of the accounts`() {
        val args = ReferenceExchangeReceipt.toArgs(ReferenceExchange)
        val restored = Json.decodeFromString(FxResultNavKey.serializer(), Json.encodeToString(FxResultNavKey(args)))

        assertEquals(args, restored.receipt)
        assertEquals("80000001", args.operationNumber)
        assertEquals("3.742", args.rateValue)
        assertEquals(usd("100.00"), args.debited)
        assertEquals(pen("374.20"), args.credited)
        assertEquals("Cuenta Dólares ****1177", args.sourceLabel)
        assertEquals(usd("950.20"), args.sourceBalance)
        assertEquals("Cuenta Gastos ****9034", args.destinationLabel)
        assertEquals(pen("2674.20"), args.destinationBalance)
    }
}
