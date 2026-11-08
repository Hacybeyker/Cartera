package com.hacybeyker.cartera.feature.transfer.ui.confirm

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
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder
import com.hacybeyker.cartera.feature.transfer.ui.TransferTestTags
import com.hacybeyker.cartera.feature.transfer.ui.messageRes
import com.hacybeyker.cartera.feature.transfer.ui.rejectionTitleRes

@Composable
fun TransferConfirmContent(
    uiState: TransferConfirmUiState,
    onAction: (TransferConfirmAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().testTag(TransferTestTags.CONFIRMATION)) {
        BackTopBar(title = stringResource(R.string.transfer_confirm_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Summary(uiState)
            Button(
                onClick = { onAction(TransferConfirmAction.ConfirmClicked) },
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TransferTestTags.CONFIRM)
            ) {
                Text(text = stringResource(R.string.transfer_confirm))
            }
            OutlinedButton(
                onClick = { onAction(TransferConfirmAction.CancelClicked) },
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TransferTestTags.CANCEL)
            ) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    }
    ConfirmDialogs(uiState = uiState, onAction = onAction)
}

@Composable
private fun Summary(uiState: TransferConfirmUiState) {
    val order = uiState.order
    SummaryRow(
        label = stringResource(R.string.transfer_summary_source),
        value = "${order.sourceAlias} ${order.sourceNumberMasked}",
        field = TransferField.SOURCE,
        uiState = uiState
    )
    SummaryRow(
        label = stringResource(R.string.transfer_summary_destination),
        value = order.destinationAccountNumber,
        field = TransferField.DESTINATION,
        uiState = uiState
    )
    SummaryRow(
        label = stringResource(R.string.transfer_summary_amount),
        value = order.amount.formatted(),
        field = TransferField.AMOUNT,
        uiState = uiState
    )
    order.description?.let {
        SummaryRow(
            label = stringResource(R.string.transfer_summary_description),
            value = it,
            field = TransferField.DESCRIPTION,
            uiState = uiState
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, field: TransferField, uiState: TransferConfirmUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        uiState.fieldErrors[field]?.let { error ->
            Text(
                text = stringResource(error.messageRes(field)),
                modifier = Modifier.testTag(TransferTestTags.fieldError(field)),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ConfirmDialogs(uiState: TransferConfirmUiState, onAction: (TransferConfirmAction) -> Unit) {
    if (uiState.isSubmitting) BlockingProgress()
    uiState.dialogError?.let { error ->
        ErrorDialog(
            title = stringResource(R.string.transfer_error_title),
            message = stringResource(error.messageRes()),
            onRetry = { onAction(TransferConfirmAction.RetryClicked) },
            onCancel = { onAction(TransferConfirmAction.ErrorDismissed) }
        )
    }
    uiState.rejection?.let { error ->
        NoticeDialog(
            testTag = TransferTestTags.rejection(error),
            title = stringResource(error.rejectionTitleRes()),
            message = stringResource(error.messageRes()),
            confirmLabel = stringResource(R.string.transfer_back_to_form),
            onConfirm = { onAction(TransferConfirmAction.RejectionAcknowledged) }
        )
    }
    if (uiState.isCancelConfirmationVisible) {
        ConfirmationDialog(
            title = stringResource(R.string.transfer_cancel_title),
            message = stringResource(R.string.transfer_cancel_message),
            confirmLabel = stringResource(R.string.transfer_cancel_confirm),
            onConfirm = { onAction(TransferConfirmAction.CancelConfirmed) },
            onCancel = { onAction(TransferConfirmAction.CancelDismissed) }
        )
    }
}

internal val previewOrder = TransferOrder(
    sourceAccountId = "ACC-002",
    sourceAlias = "Cuenta Gastos",
    sourceNumberMasked = "****9034",
    destinationAccountNumber = "999-0000001-0-01",
    amount = Money("150.00".toBigDecimal(), "PEN"),
    description = "Almuerzo",
    idempotencyKey = "preview"
)

@PreviewLightDark
@Composable
private fun TransferConfirmContentPreview() {
    CarteraTheme {
        TransferConfirmContent(uiState = TransferConfirmUiState(order = previewOrder), onAction = {}, onBack = {})
    }
}
