package com.hacybeyker.cartera.feature.movements.ui.breakdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import com.hacybeyker.cartera.core.ui.LoadUiState
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.movement.CategoryIllustration
import com.hacybeyker.cartera.core.ui.movement.labelRes
import com.hacybeyker.cartera.feature.movements.domain.model.Breakdown
import com.hacybeyker.cartera.feature.movements.domain.model.BreakdownLine
import com.hacybeyker.cartera.feature.movements.domain.model.Merchant
import com.hacybeyker.cartera.feature.movements.ui.LabeledRow
import com.hacybeyker.cartera.feature.movements.ui.MovementBreakdownTestTags

@Composable
fun MovementBreakdownContent(
    uiState: LoadUiState<Breakdown>,
    onAction: (MovementBreakdownAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().testTag(MovementBreakdownTestTags.SCREEN)) {
        BackTopBar(title = stringResource(R.string.breakdown_title), onBack = onBack)
        val error = uiState.error
        val breakdown = uiState.content
        when {
            uiState.isLoading -> LoadingState(screen = MovementBreakdownTestTags.STATES)
            error != null -> ErrorState(
                screen = MovementBreakdownTestTags.STATES,
                message = stringResource(error.messageRes()),
                onRetry = { onAction(MovementBreakdownAction.RetryClicked) }.takeIf { error.isRetryable }
            )
            breakdown != null -> BreakdownList(breakdown)
        }
    }
}

// Lista perezosa: un desglose puede traer decenas de líneas y solo se componen las visibles.
@Composable
private fun BreakdownList(breakdown: Breakdown) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = MovementBreakdownTestTags.ILLUSTRATION) { BreakdownHeader(breakdown) }
        itemsIndexed(items = breakdown.lines) { index, line ->
            LabeledRow(
                label = line.concept,
                value = line.amount.formatted(),
                modifier = Modifier.testTag(MovementBreakdownTestTags.line(index + 1))
            )
        }
        item(key = "totales") { BreakdownTotals(breakdown) }
    }
}

@Composable
private fun BreakdownHeader(breakdown: Breakdown) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryIllustration(
            category = breakdown.category,
            size = MaterialTheme.sizes.illustration,
            modifier = Modifier.testTag(MovementBreakdownTestTags.ILLUSTRATION)
        )
        Text(text = stringResource(breakdown.category.labelRes()), style = MaterialTheme.typography.titleMedium)
        breakdown.merchant?.let { merchant ->
            Text(
                text = stringResource(R.string.breakdown_merchant, merchant.name, merchant.city),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun BreakdownTotals(breakdown: Breakdown) {
    HorizontalDivider()
    LabeledRow(label = stringResource(R.string.breakdown_fees), value = breakdown.fees.formatted())
    LabeledRow(label = stringResource(R.string.breakdown_taxes), value = breakdown.taxes.formatted())
    breakdown.exchangeRate?.let { rate ->
        LabeledRow(label = stringResource(R.string.breakdown_exchange_rate), value = rate.toPlainString())
    }
}

@PreviewLightDark
@Composable
private fun MovementBreakdownContentPreview() {
    val pen = { value: String -> Money(value.toBigDecimal(), "PEN") }
    CarteraTheme {
        MovementBreakdownContent(
            uiState = LoadUiState(
                isLoading = false,
                content = Breakdown(
                    lines = listOf(
                        BreakdownLine("Arroz extra 5 kg", pen("9.53")),
                        BreakdownLine("Huevos x15", pen("29.24"))
                    ),
                    fees = pen("0.00"),
                    taxes = pen("210.39"),
                    exchangeRate = null,
                    merchant = Merchant("Supermercado La Canasta", "Lima"),
                    category = MovementCategory.GROCERIES
                )
            ),
            onAction = {},
            onBack = {}
        )
    }
}
