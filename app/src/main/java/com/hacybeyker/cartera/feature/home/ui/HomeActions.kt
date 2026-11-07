package com.hacybeyker.cartera.feature.home.ui

/** A dónde lleva cada acción de Home; lo decide quien arma la navegación. */
class HomeActions(
    val onAccountClick: (accountId: String) -> Unit,
    val onMovementClick: (movementId: String) -> Unit,
    val onLegacyClick: () -> Unit,
    val onLoggedOut: () -> Unit,
    val onHiddenScreenRequested: () -> Unit
)
