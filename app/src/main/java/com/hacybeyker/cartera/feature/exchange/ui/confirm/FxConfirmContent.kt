package com.hacybeyker.cartera.feature.exchange.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.BlockingProgress
import com.hacybeyker.cartera.core.designsystem.component.ConfirmationDialog
import com.hacybeyker.cartera.core.designsystem.component.ErrorDialog
import com.hacybeyker.cartera.core.designsystem.component.NoticeDialog
import com.hacybeyker.cartera.core.designsystem.component.windowRootTestTag
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.exchange.ExchangeAccount
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.core.ui.exchange.ExchangeFieldTestTags
import com.hacybeyker.cartera.core.ui.exchange.formattedRate
import com.hacybeyker.cartera.core.ui.exchange.messageRes
import com.hacybeyker.cartera.core.ui.exchange.rateLabelRes
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.feature.exchange.ui.FxTestTags
import com.hacybeyker.cartera.feature.exchange.ui.rejectionMessageRes
import com.hacybeyker.cartera.feature.exchange.ui.rejectionTitleRes
import java.time.OffsetDateTime

@Composable
fun FxConfirmContent(
    uiState: FxConfirmUiState,
    onAction: (FxConfirmAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().testTag(FxTestTags.CONFIRMATION)) {
        BackTopBar(title = stringResource(R.string.exchange_confirm_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Summary(uiState)
            Text(
                text = if (uiState.isQuoteExpired) {
                    stringResource(R.string.exchange_quote_expired_now)
                } else {
                    stringResource(R.string.exchange_quote_expires_in, uiState.remainingSeconds)
                },
                modifier = Modifier.testTag(FxTestTags.EXPIRY),
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.isQuoteExpired) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Button(
                onClick = { onAction(FxConfirmAction.ConfirmClicked) },
                enabled = !uiState.isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FxTestTags.CONFIRM)
            ) {
                Text(text = stringResource(R.string.exchange_confirm))
            }
            OutlinedButton(
                onClick = { onAction(FxConfirmAction.CancelClicked) },
                enabled = !uiState.isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FxTestTags.CANCEL)
            ) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    }
    ConfirmDialogs(uiState = uiState, onAction = onAction)
}

@Composable
private fun Summary(uiState: FxConfirmUiState) {
    val order = uiState.order
    SummaryRow(stringResource(R.string.exchange_source), order.source.label, ExchangeField.SOURCE, uiState)
    SummaryRow(
        label = stringResource(R.string.exchange_destination),
        value = order.destination.label,
        field = ExchangeField.DESTINATION,
        uiState = uiState
    )
    SummaryRow(
        label = stringResource(R.string.exchange_summary_debit),
        value = order.amount.formatted(),
        field = ExchangeField.AMOUNT,
        uiState = uiState,
        valueTag = FxTestTags.DEBIT
    )
    SummaryRow(
        label = stringResource(R.string.exchange_summary_credit),
        value = order.credit.formatted(),
        field = null,
        uiState = uiState,
        valueTag = FxTestTags.CREDIT
    )
    SummaryRow(
        label = stringResource(R.string.exchange_summary_rate),
        value = stringResource(
            R.string.exchange_summary_rate_value,
            order.rate.formattedRate(),
            stringResource(order.direction.rateLabelRes())
        ),
        field = ExchangeField.QUOTE,
        uiState = uiState,
        valueTag = FxTestTags.RATE
    )
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    field: ExchangeField?,
    uiState: FxConfirmUiState,
    valueTag: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier,
            style = MaterialTheme.typography.titleMedium
        )
        val error = field?.let { uiState.fieldErrors[it] }
        if (field != null && error != null) {
            Text(
                text = stringResource(error.messageRes(field)),
                modifier = Modifier.testTag(ExchangeFieldTestTags.fieldError(field)),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ConfirmDialogs(uiState: FxConfirmUiState, onAction: (FxConfirmAction) -> Unit) {
    if (uiState.isBusy) BlockingProgress()
    uiState.failure?.let { failure ->
        ErrorDialog(
            title = stringResource(
                if (failure.step == FxConfirmStep.CONFIRM) {
                    R.string.exchange_error_title
                } else {
                    R.string.exchange_quote_error_title
                }
            ),
            message = stringResource(failure.error.messageRes()),
            onRetry = { onAction(FxConfirmAction.RetryClicked) },
            onCancel = { onAction(FxConfirmAction.ErrorDismissed) }
        )
    }
    uiState.rejection?.let { error ->
        NoticeDialog(
            testTag = FxTestTags.rejection(error),
            title = stringResource(error.rejectionTitleRes()),
            message = stringResource(error.rejectionMessageRes()),
            confirmLabel = stringResource(R.string.exchange_back_to_form),
            onConfirm = { onAction(FxConfirmAction.RejectionAcknowledged) }
        )
    }
    val newQuote = uiState.newQuote
    val newCredit = uiState.newCredit
    if (newQuote != null && newCredit != null) {
        QuoteExpiredDialog(
            rate = uiState.order.direction.rate(newQuote).formattedRate(),
            credit = newCredit,
            onAccept = { onAction(FxConfirmAction.NewQuoteAccepted) },
            onExit = { onAction(FxConfirmAction.NewQuoteDeclined) }
        )
    }
    if (uiState.isCancelConfirmationVisible) {
        ConfirmationDialog(
            title = stringResource(R.string.exchange_cancel_title),
            message = stringResource(R.string.exchange_cancel_message),
            confirmLabel = stringResource(R.string.exchange_cancel_confirm),
            onConfirm = { onAction(FxConfirmAction.CancelConfirmed) },
            onCancel = { onAction(FxConfirmAction.CancelDismissed) }
        )
    }
}

/** La cotización venció: la nueva y lo que se acreditaría con ella. Tocar fuera no la cierra; Atrás es Salir. */
@Composable
private fun QuoteExpiredDialog(rate: String, credit: Money, onAccept: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onExit,
        modifier = Modifier.windowRootTestTag(FxTestTags.QUOTE_EXPIRED),
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(text = stringResource(R.string.exchange_quote_expired_title)) },
        text = { Text(text = stringResource(R.string.exchange_quote_expired_message, rate, credit.formatted())) },
        confirmButton = {
            TextButton(onClick = onAccept, modifier = Modifier.testTag(FxTestTags.QUOTE_EXPIRED_ACCEPT)) {
                Text(text = stringResource(R.string.exchange_quote_expired_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onExit, modifier = Modifier.testTag(FxTestTags.QUOTE_EXPIRED_EXIT)) {
                Text(text = stringResource(R.string.exchange_quote_expired_exit))
            }
        }
    )
}

/** El cambio de referencia: US$ 100.00 de la Cuenta Dólares a la Cuenta Gastos, a la compra inicial. */
internal val previewExchangeOrder = ExchangeOrder(
    direction = ExchangeDirection.USD_TO_PEN,
    quoteId = "QTE-000002",
    rate = "3.742".toBigDecimal(),
    expiresAt = OffsetDateTime.parse("2026-11-07T10:00:30-05:00"),
    source = ExchangeAccount("ACC-003", "Cuenta Dólares", "****1177", "USD"),
    destination = ExchangeAccount("ACC-002", "Cuenta Gastos", "****9034", "PEN"),
    amount = Money("100.00".toBigDecimal(), "USD"),
    idempotencyKey = "preview"
)

/** Una cotización nueva, un poco más alta que la inicial. */
internal val previewNewQuote = FxQuote(
    pair = FxQuote.USD_PEN,
    buy = "3.745".toBigDecimal(),
    sell = "3.771".toBigDecimal(),
    quoteId = "QTE-000003",
    expiresAt = OffsetDateTime.parse("2026-11-07T10:01:05-05:00"),
    ts = OffsetDateTime.parse("2026-11-07T10:00:35-05:00")
)

@PreviewLightDark
@Composable
private fun FxConfirmContentPreview() {
    CarteraTheme {
        FxConfirmContent(
            uiState = FxConfirmUiState(order = previewExchangeOrder, remainingSeconds = 25),
            onAction = {},
            onBack = {}
        )
    }
}
