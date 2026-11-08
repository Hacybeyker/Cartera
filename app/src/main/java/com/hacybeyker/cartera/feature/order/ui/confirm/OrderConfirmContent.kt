package com.hacybeyker.cartera.feature.order.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.BlockingProgress
import com.hacybeyker.cartera.core.designsystem.component.ConfirmationDialog
import com.hacybeyker.cartera.core.designsystem.component.ErrorDialog
import com.hacybeyker.cartera.core.designsystem.component.NoticeDialog
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.ui.OrderTestTags
import com.hacybeyker.cartera.feature.order.ui.labelRes
import com.hacybeyker.cartera.feature.order.ui.messageRes
import com.hacybeyker.cartera.feature.order.ui.rejectionMessageRes
import com.hacybeyker.cartera.feature.order.ui.rejectionTitleRes

@Composable
fun OrderConfirmContent(
    uiState: OrderConfirmUiState,
    onAction: (OrderConfirmAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (uiState.order.side == OrderSide.BUY) {
        R.string.order_confirm_title_buy
    } else {
        R.string.order_confirm_title_sell
    }
    Column(modifier = modifier.fillMaxSize().testTag(OrderTestTags.CONFIRMATION)) {
        BackTopBar(title = stringResource(title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Summary(uiState)
            Text(
                text = stringResource(R.string.order_price_may_vary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onAction(OrderConfirmAction.ConfirmClicked) },
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OrderTestTags.CONFIRM)
            ) {
                Text(text = stringResource(R.string.order_confirm))
            }
            OutlinedButton(
                onClick = { onAction(OrderConfirmAction.CancelClicked) },
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OrderTestTags.CANCEL)
            ) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    }
    ConfirmDialogs(uiState = uiState, onAction = onAction)
}

@Composable
private fun Summary(uiState: OrderConfirmUiState) {
    val order = uiState.order
    SummaryRow(stringResource(R.string.order_summary_stock), order.symbol, OrderField.SYMBOL, uiState)
    SummaryRow(
        label = stringResource(R.string.order_summary_side),
        value = stringResource(order.side.labelRes()),
        field = OrderField.SIDE,
        uiState = uiState
    )
    SummaryRow(
        label = stringResource(R.string.order_summary_quantity),
        value = order.quantity.toString(),
        field = OrderField.QUANTITY,
        uiState = uiState
    )
    SummaryRow(stringResource(R.string.order_summary_price), order.referencePrice.formatted(), null, uiState)
    SummaryRow(stringResource(R.string.order_summary_total), order.estimatedTotal.formatted(), null, uiState)
    SummaryRow(
        label = stringResource(R.string.order_summary_account),
        value = "${order.accountAlias} ${order.accountNumberMasked}".trim(),
        field = OrderField.ACCOUNT,
        uiState = uiState
    )
}

@Composable
private fun SummaryRow(label: String, value: String, field: OrderField?, uiState: OrderConfirmUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        val error = field?.let { uiState.fieldErrors[it] }
        if (field != null && error != null) {
            Text(
                text = stringResource(error.messageRes(field)),
                modifier = Modifier.testTag(OrderTestTags.fieldError(field)),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ConfirmDialogs(uiState: OrderConfirmUiState, onAction: (OrderConfirmAction) -> Unit) {
    if (uiState.isSubmitting) BlockingProgress()
    uiState.dialogError?.let { error ->
        ErrorDialog(
            title = stringResource(R.string.order_error_title),
            message = stringResource(error.messageRes()),
            onRetry = { onAction(OrderConfirmAction.RetryClicked) },
            onCancel = { onAction(OrderConfirmAction.ErrorDismissed) }
        )
    }
    uiState.rejection?.let { error ->
        NoticeDialog(
            testTag = OrderTestTags.rejection(error),
            title = stringResource(error.rejectionTitleRes()),
            message = stringResource(error.rejectionMessageRes()),
            confirmLabel = stringResource(R.string.order_back_to_form),
            onConfirm = { onAction(OrderConfirmAction.RejectionAcknowledged) }
        )
    }
    if (uiState.isCancelConfirmationVisible) {
        ConfirmationDialog(
            title = stringResource(R.string.order_cancel_title),
            message = stringResource(R.string.order_cancel_message),
            confirmLabel = stringResource(R.string.order_cancel_confirm),
            onConfirm = { onAction(OrderConfirmAction.CancelConfirmed) },
            onCancel = { onAction(OrderConfirmAction.CancelDismissed) }
        )
    }
}

/** La compra de referencia: 2 ANDM a US$ 42.10 desde la Cuenta Dólares. */
internal val previewMarketOrder = MarketOrder(
    symbol = "ANDM",
    side = OrderSide.BUY,
    quantity = 2,
    accountId = "ACC-003",
    accountAlias = "Cuenta Dólares",
    accountNumberMasked = "****1177",
    referencePrice = Money("42.10".toBigDecimal(), "USD"),
    idempotencyKey = "preview"
)

@PreviewLightDark
@Composable
private fun OrderConfirmContentPreview() {
    CarteraTheme {
        OrderConfirmContent(uiState = OrderConfirmUiState(order = previewMarketOrder), onAction = {}, onBack = {})
    }
}
