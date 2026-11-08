package com.hacybeyker.cartera.feature.order.ui.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.ConfirmationDialog
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.ui.account.AccountsSheet
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.market.ReconnectingBanner
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.ui.OrderTestTags
import com.hacybeyker.cartera.feature.order.ui.messageRes

@Composable
fun OrderFormContent(uiState: OrderFormUiState, onAction: (OrderFormAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().imePadding().testTag(OrderTestTags.FORM)) {
        BackTopBar(
            title = stringResource(uiState.side.titleRes(), uiState.symbol),
            onBack = { onAction(OrderFormAction.BackClicked) }
        )
        val error = uiState.error
        when {
            uiState.isLoading -> LoadingState(screen = OrderTestTags.STATES)
            error != null -> ErrorState(
                screen = OrderTestTags.STATES,
                message = stringResource(error.messageRes()),
                onRetry = { onAction(OrderFormAction.RetryClicked) }.takeIf { error.isRetryable }
            )
            else -> {
                if (uiState.isReconnecting) ReconnectingBanner(testTag = OrderTestTags.RECONNECTING)
                OrderForm(uiState = uiState, onAction = onAction)
            }
        }
    }
    OrderFormDialogs(uiState = uiState, onAction = onAction)
}

@Composable
private fun OrderForm(uiState: OrderFormUiState, onAction: (OrderFormAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        StockHeader(uiState)
        QuantityField(uiState = uiState, onAction = onAction)
        AccountField(uiState = uiState, onAction = onAction)
        TotalRow(uiState)
        Button(
            onClick = { onAction(OrderFormAction.ContinueClicked) },
            enabled = uiState.canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OrderTestTags.CONTINUE)
        ) {
            Text(text = stringResource(R.string.order_continue))
        }
    }
}

@Composable
private fun StockHeader(uiState: OrderFormUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        uiState.name?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = uiState.price?.let { stringResource(R.string.order_price, it.formatted()) }
                ?: stringResource(R.string.order_price_waiting),
            modifier = Modifier.testTag(OrderTestTags.PRICE),
            style = MaterialTheme.typography.titleLarge
        )
        if (uiState.side == OrderSide.SELL) {
            Text(
                text = pluralStringResource(R.plurals.order_held, uiState.heldQuantity, uiState.heldQuantity),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuantityField(uiState: OrderFormUiState, onAction: (OrderFormAction) -> Unit) {
    Column {
        OutlinedTextField(
            value = uiState.quantity,
            onValueChange = { onAction(OrderFormAction.QuantityChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OrderTestTags.QUANTITY),
            label = { Text(text = stringResource(R.string.order_quantity)) },
            isError = OrderField.QUANTITY in uiState.fieldErrors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
        FieldError(uiState = uiState, field = OrderField.QUANTITY)
    }
}

@Composable
private fun AccountField(uiState: OrderFormUiState, onAction: (OrderFormAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Text(text = stringResource(R.string.order_account), style = MaterialTheme.typography.titleSmall)
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onAction(OrderFormAction.AccountClicked) }
                .testTag(OrderTestTags.ACCOUNT)
        ) {
            val account = uiState.selectedAccount
            Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                if (account == null) {
                    Text(
                        text = stringResource(R.string.order_account_choose),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = "${account.alias} ${account.numberMasked}".trim(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.order_account_balance, account.availableBalance.formatted()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        FieldError(uiState = uiState, field = OrderField.ACCOUNT)
    }
}

@Composable
private fun TotalRow(uiState: OrderFormUiState) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.order_total),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = uiState.estimatedTotal?.formatted() ?: stringResource(R.string.order_total_unavailable),
            modifier = Modifier.testTag(OrderTestTags.TOTAL),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun FieldError(uiState: OrderFormUiState, field: OrderField) {
    uiState.fieldErrors[field]?.let { error ->
        Text(
            text = stringResource(error.messageRes(field)),
            modifier = Modifier.testTag(OrderTestTags.fieldError(field)),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun OrderFormDialogs(uiState: OrderFormUiState, onAction: (OrderFormAction) -> Unit) {
    if (uiState.isAccountSheetVisible) {
        AccountsSheet(
            accounts = uiState.accounts,
            selectedAccountId = uiState.accountId,
            onSelect = { onAction(OrderFormAction.AccountSelected(it)) },
            onDismiss = { onAction(OrderFormAction.AccountSheetDismissed) }
        )
    }
    if (uiState.isExitConfirmationVisible) {
        ConfirmationDialog(
            title = stringResource(
                if (uiState.side == OrderSide.BUY) R.string.order_exit_title_buy else R.string.order_exit_title_sell
            ),
            message = stringResource(R.string.order_exit_message),
            confirmLabel = stringResource(R.string.order_exit_confirm),
            onConfirm = { onAction(OrderFormAction.ExitConfirmed) },
            onCancel = { onAction(OrderFormAction.ExitCancelled) }
        )
    }
}

private fun OrderSide.titleRes(): Int = when (this) {
    OrderSide.BUY -> R.string.order_title_buy
    OrderSide.SELL -> R.string.order_title_sell
}

@PreviewLightDark
@Composable
private fun OrderFormContentPreview() {
    CarteraTheme {
        OrderFormContent(uiState = previewOrderForm, onAction = {})
    }
}
