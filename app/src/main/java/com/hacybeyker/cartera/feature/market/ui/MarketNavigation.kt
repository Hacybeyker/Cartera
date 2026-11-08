package com.hacybeyker.cartera.feature.market.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.ComingSoonState

// Destinos reservados para el Detalle de acción y Mi portafolio: hoy muestran "Próximamente" con su barra de volver.
fun EntryProviderScope<NavKey>.marketEntries(onBack: () -> Unit) {
    entry<StockDetailNavKey> { key ->
        ReservedDestination(title = key.symbol, screen = STOCK_DETAIL_SCREEN, onBack = onBack)
    }
    entry<PortfolioNavKey> {
        ReservedDestination(
            title = stringResource(R.string.portfolio_title),
            screen = PORTFOLIO_SCREEN,
            onBack = onBack
        )
    }
}

@Composable
private fun ReservedDestination(title: String, screen: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = title, onBack = onBack)
        ComingSoonState(screen = screen)
    }
}

private const val STOCK_DETAIL_SCREEN = "accion_detalle"
private const val PORTFOLIO_SCREEN = "portafolio"
