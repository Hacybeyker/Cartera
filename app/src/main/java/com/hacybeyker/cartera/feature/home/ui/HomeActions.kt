package com.hacybeyker.cartera.feature.home.ui

import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder

/** A dónde lleva cada acción de Home; lo decide quien arma la navegación. */
class HomeActions(
    val onAccountClick: (accountId: String) -> Unit,
    val onTransferClick: () -> Unit,
    val onMovementClick: (movementId: String) -> Unit,
    val onStockClick: (symbol: String) -> Unit,
    val onPortfolioClick: () -> Unit,
    val onExchangeContinue: (ExchangeOrder) -> Unit,
    val onLegacyClick: () -> Unit,
    val onSecurityClick: () -> Unit,
    val onHelpClick: () -> Unit,
    val onLoggedOut: () -> Unit,
    val onHiddenScreenRequested: () -> Unit
)
