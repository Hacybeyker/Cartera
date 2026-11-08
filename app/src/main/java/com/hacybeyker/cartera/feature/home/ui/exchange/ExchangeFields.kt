package com.hacybeyker.cartera.feature.home.ui.exchange

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.core.ui.exchange.ExchangeFieldTestTags
import com.hacybeyker.cartera.core.ui.exchange.formattedRate
import com.hacybeyker.cartera.core.ui.exchange.messageRes
import com.hacybeyker.cartera.core.ui.exchange.rateLabelRes
import com.hacybeyker.cartera.core.ui.format.formatted

// Los campos de la calculadora de Cambio, cada uno con su error debajo.

@Composable
internal fun AmountField(uiState: ExchangeUiState, onAction: (ExchangeAction) -> Unit) {
    Column {
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = { onAction(ExchangeAction.AmountChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ExchangeTestTags.AMOUNT),
            label = { Text(text = stringResource(uiState.direction.amountLabelRes())) },
            isError = ExchangeField.AMOUNT in uiState.fieldErrors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
        )
        FieldError(uiState = uiState, field = ExchangeField.AMOUNT)
    }
}

@Composable
internal fun AccountSelector(
    label: String,
    placeholder: String,
    account: Account?,
    field: ExchangeField,
    uiState: ExchangeUiState,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(ExchangeFieldTestTags.field(field))
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                if (account == null) {
                    Text(text = placeholder, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text(
                        text = "${account.alias} ${account.numberMasked}".trim(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.exchange_account_balance, account.availableBalance.formatted()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        FieldError(uiState = uiState, field = field)
    }
}

@Composable
internal fun Estimate(uiState: ExchangeUiState, quote: FxQuote) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.exchange_estimate),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = uiState.estimatedCredit?.formatted() ?: stringResource(R.string.exchange_estimate_unavailable),
                modifier = Modifier.testTag(ExchangeTestTags.ESTIMATE),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Text(
            text = stringResource(
                R.string.exchange_applied_rate,
                uiState.direction.rate(quote).formattedRate(),
                stringResource(uiState.direction.rateLabelRes())
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun FieldError(uiState: ExchangeUiState, field: ExchangeField) {
    uiState.fieldErrors[field]?.let { error ->
        Text(
            text = stringResource(error.messageRes(field)),
            modifier = Modifier.testTag(ExchangeFieldTestTags.fieldError(field)),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

internal fun ExchangeDirection.amountLabelRes(): Int = when (this) {
    ExchangeDirection.USD_TO_PEN -> R.string.exchange_amount_usd
    ExchangeDirection.PEN_TO_USD -> R.string.exchange_amount_pen
}
