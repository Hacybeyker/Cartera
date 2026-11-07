package com.hacybeyker.cartera.feature.movements.domain.model

import com.hacybeyker.cartera.core.domain.movement.Movement

data class MovementDetail(
    val movement: Movement,
    val operationNumber: String,
    val channel: String,
    val status: String,
    val counterpartyName: String,
    val counterpartyReference: String
)
