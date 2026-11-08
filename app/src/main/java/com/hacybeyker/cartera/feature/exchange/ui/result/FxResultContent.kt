package com.hacybeyker.cartera.feature.exchange.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.feature.exchange.ui.ExchangeReceiptArgs
import com.hacybeyker.cartera.feature.exchange.ui.FxTestTags

/** Sin [receipt], el cambio no se pudo confirmar. */
@Composable
fun FxResultContent(receipt: ExchangeReceiptArgs?, onBackToExchange: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.screen)
            .testTag(FxTestTags.RESULT),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (receipt != null) Success(receipt) else Failure()
        Button(
            onClick = onBackToExchange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(FxTestTags.BACK)
        ) {
            Text(text = stringResource(R.string.exchange_back_to_exchange))
        }
    }
}

@Composable
private fun Success(receipt: ExchangeReceiptArgs) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(MaterialTheme.sizes.logo),
        tint = MaterialTheme.colorScheme.primary
    )
    Text(text = stringResource(R.string.exchange_success_title), style = MaterialTheme.typography.headlineSmall)
    ResultLine(
        text = stringResource(R.string.exchange_success_operation, receipt.operationNumber),
        testTag = FxTestTags.RESULT_OPERATION
    )
    ResultLine(
        text = stringResource(R.string.exchange_success_debited, receipt.debited.formatted()),
        testTag = FxTestTags.RESULT_DEBITED
    )
    ResultLine(
        text = stringResource(R.string.exchange_success_credited, receipt.credited.formatted()),
        testTag = FxTestTags.RESULT_CREDITED
    )
    ResultLine(
        text = stringResource(R.string.exchange_success_rate, receipt.rateValue),
        testTag = FxTestTags.RESULT_RATE
    )
    ResultLine(
        text = stringResource(
            R.string.exchange_success_balance,
            receipt.sourceLabel,
            receipt.sourceBalance.formatted()
        ),
        testTag = FxTestTags.RESULT_SOURCE_BALANCE
    )
    ResultLine(
        text = stringResource(
            R.string.exchange_success_balance,
            receipt.destinationLabel,
            receipt.destinationBalance.formatted()
        ),
        testTag = FxTestTags.RESULT_DESTINATION_BALANCE
    )
}

@Composable
private fun ResultLine(text: String, testTag: String) {
    Text(
        text = text,
        modifier = Modifier.testTag(testTag),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Failure() {
    Icon(
        imageVector = Icons.Filled.Warning,
        contentDescription = null,
        modifier = Modifier.size(MaterialTheme.sizes.logo),
        tint = MaterialTheme.colorScheme.error
    )
    Text(
        text = stringResource(R.string.exchange_failure_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(R.string.exchange_failure_message),
        modifier = Modifier.testTag(FxTestTags.RESULT_ERROR),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

/** El resultado del cambio de referencia (`tick_ms 0`, datos limpios). */
internal val previewExchangeReceipt = ExchangeReceiptArgs(
    operationNumber = "80000001",
    rateValue = "3.742",
    debitedValue = "100.00",
    debitedCurrency = "USD",
    creditedValue = "374.20",
    creditedCurrency = "PEN",
    sourceLabel = "Cuenta Dólares ****1177",
    sourceBalanceValue = "950.20",
    destinationLabel = "Cuenta Gastos ****9034",
    destinationBalanceValue = "2674.20"
)

@PreviewLightDark
@Composable
private fun FxResultContentPreview() {
    CarteraTheme {
        FxResultContent(receipt = previewExchangeReceipt, onBackToExchange = {})
    }
}
