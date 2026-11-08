package com.hacybeyker.cartera.feature.home.ui.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BlockingProgress
import com.hacybeyker.cartera.core.designsystem.component.ErrorDialog
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.core.ui.account.AccountsSheet
import com.hacybeyker.cartera.core.ui.exchange.formattedRate
import com.hacybeyker.cartera.core.ui.exchange.messageRes
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.market.ReconnectingBanner
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.core.ui.preview.previewFxQuote

/** Cambio: compra y venta en vivo y la calculadora con monto, sentido y cuentas de origen y destino. */
@Composable
fun ExchangeContent(uiState: ExchangeUiState, onAction: (ExchangeAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().imePadding().testTag(ExchangeTestTags.SCREEN)) {
        val error = uiState.error
        val quote = uiState.quote
        when {
            !uiState.isLoading && error != null -> ErrorState(
                screen = ExchangeTestTags.EXCHANGE,
                message = stringResource(error.messageRes()),
                onRetry = { onAction(ExchangeAction.RetryClicked) }.takeIf { error.isRetryable }
            )
            uiState.isLoading || quote == null -> LoadingState(screen = ExchangeTestTags.EXCHANGE)
            else -> {
                if (uiState.isReconnecting) ReconnectingBanner(testTag = ExchangeTestTags.RECONNECTING)
                Calculator(uiState = uiState, quote = quote, onAction = onAction)
            }
        }
    }
    ExchangeDialogs(uiState = uiState, onAction = onAction)
}

@Composable
private fun Calculator(uiState: ExchangeUiState, quote: FxQuote, onAction: (ExchangeAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        Text(text = stringResource(R.string.exchange_title), style = MaterialTheme.typography.headlineSmall)
        Rates(quote)
        DirectionButton(direction = uiState.direction, onClick = { onAction(ExchangeAction.DirectionToggled) })
        AmountField(uiState = uiState, onAction = onAction)
        AccountSelector(
            label = stringResource(R.string.exchange_source),
            placeholder = stringResource(R.string.exchange_source_choose),
            account = uiState.sourceAccount,
            field = ExchangeField.SOURCE,
            uiState = uiState,
            onClick = { onAction(ExchangeAction.AccountClicked(ExchangeAccountRole.SOURCE)) }
        )
        AccountSelector(
            label = stringResource(R.string.exchange_destination),
            placeholder = stringResource(R.string.exchange_destination_choose),
            account = uiState.destinationAccount,
            field = ExchangeField.DESTINATION,
            uiState = uiState,
            onClick = { onAction(ExchangeAction.AccountClicked(ExchangeAccountRole.DESTINATION)) }
        )
        Estimate(uiState = uiState, quote = quote)
        TextButton(
            onClick = { onAction(ExchangeAction.HowWeCalculateClicked) },
            modifier = Modifier.testTag(ExchangeTestTags.HOW_WE_CALCULATE)
        ) {
            Text(text = stringResource(R.string.exchange_how_we_calculate))
        }
        Button(
            onClick = { onAction(ExchangeAction.ContinueClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ExchangeTestTags.CONTINUE)
        ) {
            Text(text = stringResource(R.string.exchange_continue))
        }
    }
}

@Composable
private fun Rates(quote: FxQuote) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.exchange_rates_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                Rate(
                    label = stringResource(R.string.exchange_rate_buy),
                    value = quote.buy.formattedRate(),
                    testTag = ExchangeTestTags.BUY,
                    modifier = Modifier.weight(1f)
                )
                Rate(
                    label = stringResource(R.string.exchange_rate_sell),
                    value = quote.sell.formattedRate(),
                    testTag = ExchangeTestTags.SELL,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun Rate(label: String, value: String, testTag: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // El testTag va solo en el número, así un flow lee la tasa exacta.
        Text(text = value, modifier = Modifier.testTag(testTag), style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun DirectionButton(direction: ExchangeDirection, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ExchangeTestTags.DIRECTION),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Text(text = stringResource(direction.labelRes()), modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Filled.Refresh, contentDescription = stringResource(R.string.exchange_direction_swap))
    }
}

@Composable
private fun ExchangeDialogs(uiState: ExchangeUiState, onAction: (ExchangeAction) -> Unit) {
    uiState.accountSheetFor?.let { role ->
        AccountsSheet(
            accounts = uiState.accountsFor(role),
            selectedAccountId = when (role) {
                ExchangeAccountRole.SOURCE -> uiState.sourceAccountId
                ExchangeAccountRole.DESTINATION -> uiState.destinationAccountId
            },
            onSelect = { onAction(ExchangeAction.AccountSelected(it)) },
            onDismiss = { onAction(ExchangeAction.SheetDismissed) }
        )
    }
    val quote = uiState.quote
    if (uiState.isHowWeCalculateVisible && quote != null) {
        HowWeCalculateSheet(quote = quote, onDismiss = { onAction(ExchangeAction.SheetDismissed) })
    }
    if (uiState.isPreparing) BlockingProgress()
    uiState.dialogError?.let { error ->
        ErrorDialog(
            title = stringResource(R.string.exchange_quote_error_title),
            message = stringResource(error.messageRes()),
            onRetry = { onAction(ExchangeAction.ContinueClicked) },
            onCancel = { onAction(ExchangeAction.ErrorDismissed) }
        )
    }
}

private fun ExchangeDirection.labelRes(): Int = when (this) {
    ExchangeDirection.USD_TO_PEN -> R.string.exchange_direction_usd_to_pen
    ExchangeDirection.PEN_TO_USD -> R.string.exchange_direction_pen_to_usd
}

/** El cambio de referencia a medio llenar: US$ 100.00 desde la Cuenta Dólares, a la tasa inicial. */
internal val previewExchange = ExchangeUiState(
    isLoading = false,
    accounts = previewAccounts,
    quote = previewFxQuote,
    amount = "100",
    sourceAccountId = "ACC-003",
    destinationAccountId = "ACC-002",
    contentVersion = 1
)

@PreviewLightDark
@Composable
private fun ExchangeContentPreview() {
    CarteraTheme {
        ExchangeContent(uiState = previewExchange, onAction = {})
    }
}
