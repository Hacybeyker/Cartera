package com.hacybeyker.cartera.feature.transfer.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.ui.TransferTestTags
import com.hacybeyker.cartera.feature.transfer.ui.messageRes

@Composable
fun TransferFormContent(
    uiState: TransferFormUiState,
    onAction: (TransferFormAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().imePadding().testTag(TransferTestTags.FORM)) {
        BackTopBar(
            title = stringResource(R.string.transfer_title),
            onBack = { onAction(TransferFormAction.BackClicked) }
        )
        val error = uiState.error
        when {
            uiState.isLoading -> LoadingState(screen = TransferTestTags.STATES)
            error != null -> ErrorState(
                screen = TransferTestTags.STATES,
                message = stringResource(error.messageRes()),
                onRetry = { onAction(TransferFormAction.RetryClicked) }.takeIf { error.isRetryable }
            )
            else -> TransferForm(uiState = uiState, onAction = onAction)
        }
    }
    if (uiState.isExitConfirmationVisible) {
        ConfirmationDialog(
            title = stringResource(R.string.transfer_exit_title),
            message = stringResource(R.string.transfer_exit_message),
            confirmLabel = stringResource(R.string.transfer_exit_confirm),
            onConfirm = { onAction(TransferFormAction.ExitConfirmed) },
            onCancel = { onAction(TransferFormAction.ExitCancelled) }
        )
    }
}

@Composable
private fun TransferForm(uiState: TransferFormUiState, onAction: (TransferFormAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        SourceSelector(uiState = uiState, onAction = onAction)
        FormField(
            field = TransferField.DESTINATION,
            label = stringResource(R.string.transfer_destination),
            uiState = uiState,
            onAction = onAction,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
        )
        FormField(
            field = TransferField.AMOUNT,
            label = uiState.currency?.let { stringResource(R.string.transfer_amount_in, it) }
                ?: stringResource(R.string.transfer_amount),
            uiState = uiState,
            onAction = onAction,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
        )
        FormField(
            field = TransferField.DESCRIPTION,
            label = stringResource(R.string.transfer_description),
            uiState = uiState,
            onAction = onAction,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Button(
            onClick = { onAction(TransferFormAction.ContinueClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TransferTestTags.CONTINUE)
        ) {
            Text(text = stringResource(R.string.transfer_continue))
        }
    }
}

@Composable
private fun SourceSelector(uiState: TransferFormUiState, onAction: (TransferFormAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Text(text = stringResource(R.string.transfer_source), style = MaterialTheme.typography.titleSmall)
        Column(modifier = Modifier.selectableGroup().testTag(TransferTestTags.SOURCE)) {
            uiState.accounts.forEach { account ->
                SourceOption(
                    account = account,
                    selected = account.id == uiState.sourceAccountId,
                    onSelect = { onAction(TransferFormAction.SourceSelected(account.id)) }
                )
            }
        }
        FieldError(uiState = uiState, field = TransferField.SOURCE)
    }
}

@Composable
private fun SourceOption(account: Account, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = MaterialTheme.spacing.xs)
            .testTag(TransferTestTags.source(account.id)),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = account.alias, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = account.numberMasked,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = account.availableBalance.formatted(), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FormField(
    field: TransferField,
    label: String,
    uiState: TransferFormUiState,
    onAction: (TransferFormAction) -> Unit,
    keyboardOptions: KeyboardOptions
) {
    Column {
        OutlinedTextField(
            value = uiState.valueOf(field),
            onValueChange = { onAction(changeOf(field, it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TransferTestTags.field(field)),
            label = { Text(text = label) },
            placeholder = if (field == TransferField.DESTINATION) {
                { Text(text = stringResource(R.string.transfer_destination_hint)) }
            } else {
                null
            },
            isError = field in uiState.fieldErrors,
            singleLine = true,
            keyboardOptions = keyboardOptions
        )
        FieldError(uiState = uiState, field = field)
    }
}

private fun TransferFormUiState.valueOf(field: TransferField): String = when (field) {
    TransferField.DESTINATION -> destination
    TransferField.AMOUNT -> amount
    TransferField.DESCRIPTION -> description
    TransferField.SOURCE -> sourceAccountId.orEmpty()
}

private fun changeOf(field: TransferField, value: String): TransferFormAction = when (field) {
    TransferField.DESTINATION -> TransferFormAction.DestinationChanged(value)
    TransferField.AMOUNT -> TransferFormAction.AmountChanged(value)
    TransferField.DESCRIPTION -> TransferFormAction.DescriptionChanged(value)
    TransferField.SOURCE -> TransferFormAction.SourceSelected(value)
}

@Composable
private fun FieldError(uiState: TransferFormUiState, field: TransferField) {
    uiState.fieldErrors[field]?.let { error ->
        Text(
            text = stringResource(error.messageRes(field)),
            modifier = Modifier.testTag(TransferTestTags.fieldError(field)),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@PreviewLightDark
@Composable
private fun TransferFormContentPreview() {
    CarteraTheme {
        TransferFormContent(
            uiState = TransferFormUiState(
                isLoading = false,
                accounts = previewAccounts,
                sourceAccountId = "ACC-002",
                destination = "999-0000001-0-01",
                amount = "150.00"
            ),
            onAction = {}
        )
    }
}
