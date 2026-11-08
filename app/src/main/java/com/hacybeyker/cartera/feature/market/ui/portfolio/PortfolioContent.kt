package com.hacybeyker.cartera.feature.market.ui.portfolio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.EmptyState
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.format.formattedPercent
import com.hacybeyker.cartera.core.ui.format.formattedSigned
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.market.ReconnectingBanner
import com.hacybeyker.cartera.core.ui.market.trendColor
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.preview.previewPortfolio

/** Mi portafolio: totales arriba y cada tenencia con su valor y su ganancia o pérdida en vivo. */
@Composable
fun PortfolioContent(
    uiState: PortfolioUiState,
    onAction: (PortfolioAction) -> Unit,
    onBack: () -> Unit,
    onStockClick: (symbol: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        BackTopBar(title = stringResource(R.string.portfolio_title), onBack = onBack)
        val error = uiState.error
        val summary = uiState.summary
        when {
            uiState.isLoading -> LoadingState(screen = PortfolioTestTags.PORTFOLIO)
            error != null -> ErrorState(
                screen = PortfolioTestTags.PORTFOLIO,
                message = stringResource(error.messageRes()),
                onRetry = { onAction(PortfolioAction.RetryClicked) }.takeIf { error.isRetryable }
            )
            uiState.holdings.isEmpty() || summary == null -> EmptyState(
                screen = PortfolioTestTags.PORTFOLIO,
                message = stringResource(R.string.portfolio_empty)
            )
            else -> {
                if (uiState.isReconnecting) ReconnectingBanner(testTag = PortfolioTestTags.RECONNECTING)
                SummaryHeader(summary = summary)
                HorizontalDivider()
                HoldingList(holdings = uiState.holdings, onStockClick = onStockClick)
            }
        }
    }
}

@Composable
private fun SummaryHeader(summary: PortfolioSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
    ) {
        Text(
            text = stringResource(R.string.portfolio_market_value),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = summary.marketValue.formatted(), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(
                R.string.portfolio_total_gain,
                summary.gain.formattedSigned(),
                summary.gainPct.formattedPercent()
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = trendColor(summary.gain.amount)
        )
        Text(
            text = stringResource(
                R.string.market_portfolio_day_change,
                summary.dayChange.formattedSigned(),
                summary.dayChangePct.formattedPercent()
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = trendColor(summary.dayChange.amount)
        )
    }
}

@Composable
private fun HoldingList(holdings: List<HoldingItem>, onStockClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(PortfolioTestTags.LIST)) {
        // La clave estable por símbolo hace que un tick recomponga solo la fila de esa acción.
        items(items = holdings, key = { it.symbol }, contentType = { HOLDING_ROW }) { holding ->
            HoldingRow(holding = holding, onClick = { onStockClick(holding.symbol) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun HoldingRow(holding: HoldingItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.screen, vertical = MaterialTheme.spacing.md)
            .testTag(PortfolioTestTags.holding(holding.symbol)),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = holding.symbol, style = MaterialTheme.typography.titleMedium)
            Text(
                text = holding.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(
                    R.plurals.portfolio_item_shares,
                    holding.quantity,
                    holding.quantity,
                    holding.averagePrice.formatted()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = holding.marketValue.formatted(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.market_change,
                    holding.gain.formattedSigned(),
                    holding.gainPct.formattedPercent()
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = trendColor(holding.gain.amount)
            )
            Text(
                text = stringResource(R.string.portfolio_item_price, holding.lastPrice.formatted()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val HOLDING_ROW = "holding"

@PreviewLightDark
@Composable
private fun PortfolioContentPreview() {
    CarteraTheme {
        PortfolioContent(
            uiState = PortfolioUiState().loaded(previewPortfolio),
            onAction = {},
            onBack = {},
            onStockClick = {}
        )
    }
}
