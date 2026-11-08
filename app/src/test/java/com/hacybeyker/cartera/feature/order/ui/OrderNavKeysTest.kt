package com.hacybeyker.cartera.feature.order.ui

import com.hacybeyker.cartera.feature.order.ReferenceBuy
import com.hacybeyker.cartera.feature.order.ReferenceBuyReceipt
import com.hacybeyker.cartera.feature.order.domain.model.OrderHolding
import com.hacybeyker.cartera.feature.order.usd
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderNavKeysTest {
    @Test
    fun `the order survives the trip through its nav key, idempotency key included`() {
        val key = OrderConfirmNavKey(ReferenceBuy.toArgs())

        val restored = Json.decodeFromString(OrderConfirmNavKey.serializer(), Json.encodeToString(key))

        assertEquals(ReferenceBuy, restored.order.toOrder())
    }

    @Test
    fun `the receipt keeps what the result shows`() {
        val args = ReferenceBuyReceipt.toArgs()

        assertEquals(usd("42.10"), args.price)
        assertEquals(usd("84.20"), args.total)
        assertEquals(usd("966.00"), args.balance)
        assertEquals(12, args.holdingQuantity)
        assertEquals(usd("40.35"), args.holdingAverage)
        val emptied = ReferenceBuyReceipt.copy(holding = OrderHolding(0, null)).toArgs()
        assertNull(emptied.holdingAverage)
    }
}
