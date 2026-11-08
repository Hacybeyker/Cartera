package com.hacybeyker.cartera.feature.order.domain.model

sealed interface OrderValidation {
    data class Valid(val order: MarketOrder) : OrderValidation

    data class Invalid(val errors: Map<OrderField, OrderFieldError>) : OrderValidation
}
