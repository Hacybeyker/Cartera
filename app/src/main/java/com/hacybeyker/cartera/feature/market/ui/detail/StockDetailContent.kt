package com.hacybeyker.cartera.feature.market.ui.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.format.formattedChange
import com.hacybeyker.cartera.core.ui.format.formattedPercent
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.market.ReconnectingBanner
import com.hacybeyker.cartera.core.ui.market.trendColor
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.preview.previewPortfolio
import com.hacybeyker.cartera.core.ui.preview.previewQuotes

/**
 * Detalle de acción: precio en vivo, gráfico con rangos, datos del día y la tenencia propia si la hay. Operar abre el
 * sheet Comprar/Vender y lo elegido sale por [onTrade]. [chartLivePath] elige el camino de dibujo del gráfico.
 */
@Composable
fun StockDetailContent(
    symbol: String,
    uiState: StockDetailUiState,
    onAction: (StockDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onTrade: (OrderSide) -> Unit = {},
    chartLivePath: Boolean = false
) {
    Column(modifier = modifier.fillMaxSize().testTag(StockDetailTestTags.SCREEN)) {
        BackTopBar(title = symbol, onBack = onBack)
        val error = uiState.error
        when {
            uiState.isLoading -> LoadingState(screen = StockDetailTestTags.STATES)
            error != null -> ErrorState(
                screen = StockDetailTestTags.STATES,
                message = stringResource(error.messageRes()),
                onRetry = { onAction(StockDetailAction.RetryClicked) }.takeIf { error.isRetryable }
            )
            else -> uiState.price?.let {
                DetailBody(uiState = uiState, price = it, chartLivePath = chartLivePath, onAction = onAction)
            }
        }
    }
    if (uiState.isTradeSheetVisible) {
        TradeSheet(
            symbol = symbol,
            canSell = uiState.canSell,
            onChoose = { side ->
                onAction(StockDetailAction.TradeSheetDismissed)
                onTrade(side)
            },
            onDismiss = { onAction(StockDetailAction.TradeSheetDismissed) }
        )
    }
}

@Composable
private fun DetailBody(
    uiState: StockDetailUiState,
    price: LivePrice,
    chartLivePath: Boolean,
    onAction: (StockDetailAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.isReconnecting) ReconnectingBanner(testTag = StockDetailTestTags.RECONNECTING)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            PriceHeader(name = uiState.name, price = price)
            RangeSelector(
                selected = uiState.range,
                onSelect = { onAction(StockDetailAction.RangeSelected(it)) }
            )
            ChartArea(
                chart = uiState.chart,
                currency = price.price.currency,
                livePath = chartLivePath,
                onRetry = { onAction(StockDetailAction.ChartRetryClicked) }
            )
            uiState.day?.let { DaySection(day = it) }
            uiState.holding?.let { HoldingSection(holding = it) }
        }
        // Fijo abajo, fuera del scroll: siempre a mano, aunque la tenencia quede más abajo.
        Button(
            onClick = { onAction(StockDetailAction.TradeClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.screen)
                .testTag(StockDetailTestTags.TRADE)
        ) {
            Text(text = stringResource(R.string.stock_trade))
        }
    }
}

@Composable
private fun PriceHeader(name: String, price: LivePrice) {
    Column(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = price.price.formatted(),
            modifier = Modifier.testTag(StockDetailTestTags.PRICE),
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = stringResource(
                R.string.stock_change_today,
                price.change.formattedChange(),
                price.changePct.formattedPercent()
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = trendColor(price.change)
        )
    }
}

@Composable
private fun RangeSelector(selected: HistoryRange, onSelect: (HistoryRange) -> Unit) {
    val ranges = HistoryRange.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.screen)
    ) {
        ranges.forEachIndexed { index, range ->
            SegmentedButton(
                selected = range == selected,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size),
                modifier = Modifier.testTag(StockDetailTestTags.range(range)),
                label = { Text(text = stringResource(range.labelRes())) }
            )
        }
    }
}

@StringRes
private fun HistoryRange.labelRes(): Int = when (this) {
    HistoryRange.DAY -> R.string.stock_range_day
    HistoryRange.WEEK -> R.string.stock_range_week
    HistoryRange.MONTH -> R.string.stock_range_month
    HistoryRange.YEAR -> R.string.stock_range_year
}

@PreviewLightDark
@Composable
private fun StockDetailContentPreview() {
    val quote = previewQuotes.last()
    CarteraTheme {
        StockDetailContent(
            symbol = quote.symbol,
            uiState = previewStockDetailState(quote, previewPortfolio.holdings.last()),
            onAction = {},
            onBack = {}
        )
    }
}
