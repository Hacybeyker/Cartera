package com.hacybeyker.cartera.core.data.network.dto

import kotlinx.serialization.Serializable

// Solo lo que la app no puede calcular: los valores y totales se recalculan en el cliente con los precios en vivo.
@Serializable
data class PortfolioHoldingDto(
    val symbol: String,
    val name: String,
    val quantity: Int,
    val averagePrice: MoneyDto,
    val lastPrice: MoneyDto,
    val dayChange: MoneyDto
)

@Serializable
data class PortfolioDto(val accountId: String, val currency: String, val holdings: List<PortfolioHoldingDto>)
