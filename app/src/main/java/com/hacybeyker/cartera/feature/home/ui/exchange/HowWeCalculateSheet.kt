package com.hacybeyker.cartera.feature.home.ui.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.windowRootTestTag
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.core.ui.exchange.formattedRate
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.preview.previewFxQuote
import java.math.BigDecimal
import kotlinx.coroutines.launch

/**
 * "Cómo calculamos el tipo de cambio": qué es la compra y la venta, cuál se aplica en cada sentido y un ejemplo con
 * [quote], la cotización de ahora. Entendido cierra el sheet con su animación y después avisa con [onDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HowWeCalculateSheet(quote: FxQuote, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // El sheet se dibuja en otra ventana, como los diálogos.
        modifier = Modifier.windowRootTestTag(ExchangeTestTags.HOW_WE_CALCULATE_SHEET),
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.screen)
                .padding(bottom = MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Text(text = stringResource(R.string.exchange_how_title), style = MaterialTheme.typography.titleMedium)
            Paragraph(stringResource(R.string.exchange_how_buy, quote.buy.formattedRate()))
            Paragraph(stringResource(R.string.exchange_how_sell, quote.sell.formattedRate()))
            Text(text = stringResource(R.string.exchange_how_example), style = MaterialTheme.typography.titleSmall)
            Paragraph(example(ExchangeDirection.USD_TO_PEN, quote, R.string.exchange_how_example_usd_to_pen))
            Paragraph(example(ExchangeDirection.PEN_TO_USD, quote, R.string.exchange_how_example_pen_to_usd))
            Paragraph(stringResource(R.string.exchange_how_rounding))
            Button(
                onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ExchangeTestTags.HOW_WE_CALCULATE_CLOSE)
            ) {
                Text(text = stringResource(R.string.exchange_how_close))
            }
        }
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

/** "US$ 100.00 × 3.742 = S/ 374.20", con el mismo cálculo que la calculadora y el servidor. */
@Composable
private fun example(direction: ExchangeDirection, quote: FxQuote, template: Int): String {
    val rate = direction.rate(quote)
    val amount = Money(EXAMPLE_AMOUNT, direction.sourceCurrency)
    val credit = Money(direction.credit(EXAMPLE_AMOUNT, rate), direction.destinationCurrency)
    return stringResource(template, amount.formatted(), rate.formattedRate(), credit.formatted())
}

private val EXAMPLE_AMOUNT = BigDecimal("100.00")

@PreviewLightDark
@Composable
private fun HowWeCalculateSheetPreview() {
    CarteraTheme {
        HowWeCalculateSheet(quote = previewFxQuote, onDismiss = {})
    }
}
