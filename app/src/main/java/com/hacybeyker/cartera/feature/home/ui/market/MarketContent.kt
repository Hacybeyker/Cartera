package com.hacybeyker.cartera.feature.home.ui.market

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.EmptyState
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.format.formattedChange
import com.hacybeyker.cartera.core.ui.format.formattedPercent
import com.hacybeyker.cartera.core.ui.format.formattedSigned
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.market.trendColor
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.preview.previewPortfolio
import com.hacybeyker.cartera.core.ui.preview.previewQuotes

/** Mercado: cabecera de Mi portafolio y la lista de acciones, con precios que se actualizan solos. */
@Composable
fun MarketContent(
    uiState: MarketUiState,
    onAction: (MarketAction) -> Unit,
    onStockClick: (symbol: String) -> Unit,
    onPortfolioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val error = uiState.error
    when {
        uiState.isLoading -> LoadingState(screen = MarketTestTags.MARKET, modifier = modifier)
        error != null -> ErrorState(
            screen = MarketTestTags.MARKET,
            message = stringResource(error.messageRes()),
            onRetry = { onAction(MarketAction.RetryClicked) }.takeIf { error.isRetryable },
            modifier = modifier
        )
        uiState.stocks.isEmpty() -> EmptyState(
            screen = MarketTestTags.MARKET,
            message = stringResource(R.string.market_empty),
            modifier = modifier
        )
        else -> Column(modifier = modifier.fillMaxSize()) {
            if (uiState.isReconnecting) ReconnectingBanner()
            uiState.portfolio?.let { PortfolioCard(header = it, onClick = onPortfolioClick) }
            HorizontalDivider()
            StockList(stocks = uiState.stocks, onStockClick = onStockClick)
        }
    }
}

@Composable
private fun ReconnectingBanner() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MarketTestTags.RECONNECTING)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screen, vertical = MaterialTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(MaterialTheme.sizes.inlineProgress),
                strokeWidth = MaterialTheme.sizes.inlineProgressStroke
            )
            Text(text = stringResource(R.string.market_reconnecting), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PortfolioCard(header: PortfolioHeader, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(MaterialTheme.spacing.screen)
            .testTag(MarketTestTags.PORTFOLIO),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.market_portfolio_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = header.marketValue.formatted(), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(
                    R.string.market_portfolio_day_change,
                    header.dayChange.formattedSigned(),
                    header.dayChangePct.formattedPercent()
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = trendColor(header.dayChange.amount)
            )
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun StockList(stocks: List<StockItem>, onStockClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(MarketTestTags.LIST)) {
        // La clave estable por símbolo hace que un tick recomponga solo la fila de esa acción.
        items(items = stocks, key = { it.symbol }, contentType = { STOCK_ROW }) { stock ->
            StockRow(stock = stock, onClick = { onStockClick(stock.symbol) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun StockRow(stock: StockItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.screen, vertical = MaterialTheme.spacing.md)
            .testTag(MarketTestTags.stock(stock.symbol)),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stock.symbol, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stock.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = stock.price.formatted(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.market_change,
                    stock.change.formattedChange(),
                    stock.changePct.formattedPercent()
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = trendColor(stock.change)
            )
        }
    }
}

private const val STOCK_ROW = "stock"

@PreviewLightDark
@Composable
private fun MarketContentPreview() {
    CarteraTheme {
        MarketContent(
            uiState = MarketUiState(
                isLoading = false,
                stocks = previewQuotes.map(StockItem::from),
                portfolio = PortfolioHeader.from(previewPortfolio)
            ),
            onAction = {},
            onStockClick = {},
            onPortfolioClick = {}
        )
    }
}
