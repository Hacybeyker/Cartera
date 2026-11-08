package com.hacybeyker.cartera.feature.exchange.domain.repository

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.feature.exchange.domain.model.ExchangeReceipt

interface ExchangeRepository {
    suspend fun exchange(order: ExchangeOrder): Outcome<ExchangeReceipt>
}
