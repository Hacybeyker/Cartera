package com.hacybeyker.cartera.feature.movements.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class AccountMovementsNavKey(val accountId: String) : NavKey

@Serializable
data class MovementDetailNavKey(val movementId: String) : NavKey

@Serializable
data class MovementBreakdownNavKey(val movementId: String) : NavKey
