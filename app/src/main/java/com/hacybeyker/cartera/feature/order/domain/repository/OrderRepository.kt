package com.hacybeyker.cartera.feature.order.domain.repository

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderReceipt

interface OrderRepository {
    suspend fun place(order: MarketOrder): Outcome<OrderReceipt>
}
