package com.hacybeyker.cartera.feature.order.ui.result

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.feature.order.ui.OrderReceiptArgs
import com.hacybeyker.cartera.feature.order.ui.OrderTestTags

/** Sin [receipt], la orden no se pudo confirmar. */
@Composable
fun OrderResultContent(receipt: OrderReceiptArgs?, onBackToMarket: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.screen)
            .testTag(OrderTestTags.RESULT),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (receipt != null) Success(receipt) else Failure()
        Button(
            onClick = onBackToMarket,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OrderTestTags.BACK_TO_MARKET)
        ) {
            Text(text = stringResource(R.string.order_back_to_market))
        }
    }
}

@Composable
private fun Success(receipt: OrderReceiptArgs) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(MaterialTheme.sizes.logo),
        tint = MaterialTheme.colorScheme.primary
    )
    Text(
        text = stringResource(
            if (receipt.side == OrderSide.BUY) R.string.order_success_title_buy else R.string.order_success_title_sell
        ),
        style = MaterialTheme.typography.headlineSmall
    )
    ResultLine(
        text = stringResource(R.string.order_success_operation, receipt.operationNumber),
        testTag = OrderTestTags.RESULT_OPERATION
    )
    ResultLine(
        text = stringResource(R.string.order_success_price, receipt.price.formatted()),
        testTag = OrderTestTags.RESULT_PRICE
    )
    ResultLine(
        text = stringResource(R.string.order_success_total, receipt.total.formatted()),
        testTag = OrderTestTags.RESULT_TOTAL
    )
    ResultLine(
        text = stringResource(R.string.order_success_balance, receipt.balance.formatted()),
        testTag = OrderTestTags.RESULT_BALANCE
    )
    val average = receipt.holdingAverage
    ResultLine(
        text = if (receipt.holdingQuantity > 0 && average != null) {
            pluralStringResource(
                R.plurals.order_success_holding,
                receipt.holdingQuantity,
                receipt.holdingQuantity,
                average.formatted()
            )
        } else {
            stringResource(R.string.order_success_holding_none, receipt.symbol)
        },
        testTag = OrderTestTags.RESULT_HOLDING
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
        text = stringResource(R.string.order_failure_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(R.string.order_failure_message),
        modifier = Modifier.testTag(OrderTestTags.RESULT_ERROR),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

/** El resultado de la compra de referencia (`tick_ms 0`). */
internal val previewOrderReceipt = OrderReceiptArgs(
    side = OrderSide.BUY,
    symbol = "ANDM",
    operationNumber = "70000001",
    priceValue = "42.10",
    totalValue = "84.20",
    balanceValue = "966.00",
    currency = "USD",
    holdingQuantity = 12,
    holdingAverageValue = "40.35"
)

@PreviewLightDark
@Composable
private fun OrderResultContentPreview() {
    CarteraTheme {
        OrderResultContent(receipt = previewOrderReceipt, onBackToMarket = {})
    }
}
