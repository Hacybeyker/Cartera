package com.hacybeyker.cartera.feature.market.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LabeledRow
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.format.formattedCount
import com.hacybeyker.cartera.core.ui.format.formattedPercent
import com.hacybeyker.cartera.core.ui.format.formattedSigned
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.market.trendColor
import com.hacybeyker.cartera.core.ui.messageRes

/** El gráfico del rango elegido, o su carga o su error, con el mínimo y el máximo de la serie debajo. */
@Composable
internal fun ChartArea(chart: ChartUiState, currency: String, onRetry: () -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.sizes.chartHeight)
                .padding(horizontal = MaterialTheme.spacing.screen)
        ) {
            val error = chart.error
            val series = chart.chart
            when {
                chart.isLoading -> LoadingState(screen = StockDetailTestTags.CHART_STATES)
                error != null -> ErrorState(
                    screen = StockDetailTestTags.CHART_STATES,
                    message = stringResource(error.messageRes()),
                    onRetry = onRetry.takeIf { error.isRetryable }
                )
                series != null -> PriceChartLine(
                    chart = series,
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = MaterialTheme.sizes.chartStroke,
                    description = stringResource(R.string.stock_chart_description),
                    modifier = Modifier.fillMaxSize().testTag(StockDetailTestTags.CHART)
                )
            }
        }
        chart.chart?.let { ChartLimits(chart = it, currency = currency) }
    }
}

@Composable
private fun ChartLimits(chart: PriceChart, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.screen, vertical = MaterialTheme.spacing.xs)
    ) {
        Text(
            text = stringResource(R.string.stock_chart_low, Money(chart.low, currency).formatted()),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.stock_chart_high, Money(chart.high, currency).formatted()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun DaySection(day: DayData) {
    Column {
        SectionTitle(text = stringResource(R.string.stock_day_title))
        LabeledRow(label = stringResource(R.string.stock_open), value = day.open.formatted())
        LabeledRow(label = stringResource(R.string.stock_high), value = day.high.formatted())
        LabeledRow(label = stringResource(R.string.stock_low), value = day.low.formatted())
        LabeledRow(label = stringResource(R.string.stock_previous_close), value = day.previousClose.formatted())
        LabeledRow(label = stringResource(R.string.stock_volume), value = day.volume.formattedCount())
        SectionTitle(text = stringResource(R.string.stock_about_title))
        Text(
            text = day.description,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screen),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
internal fun HoldingSection(holding: HoldingSummary) {
    Column(modifier = Modifier.testTag(StockDetailTestTags.HOLDING)) {
        SectionTitle(text = stringResource(R.string.stock_holding_title))
        LabeledRow(label = stringResource(R.string.stock_holding_quantity), value = holding.quantity.toString())
        LabeledRow(
            label = stringResource(R.string.stock_holding_average_price),
            value = holding.averagePrice.formatted()
        )
        LabeledRow(
            label = stringResource(R.string.stock_holding_market_value),
            value = holding.marketValue.formatted()
        )
        LabeledRow(
            label = stringResource(R.string.stock_holding_gain),
            value = stringResource(
                R.string.market_change,
                holding.gain.formattedSigned(),
                holding.gainPct.formattedPercent()
            ),
            valueColor = trendColor(holding.gain.amount)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column {
        HorizontalDivider()
        Text(
            text = text,
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.screen,
                end = MaterialTheme.spacing.screen,
                top = MaterialTheme.spacing.md,
                bottom = MaterialTheme.spacing.xs
            ),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
