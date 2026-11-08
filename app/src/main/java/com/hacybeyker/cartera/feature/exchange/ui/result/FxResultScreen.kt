package com.hacybeyker.cartera.feature.exchange.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.exchange.ui.ExchangeReceiptArgs
import com.hacybeyker.cartera.feature.exchange.ui.FxTestTags

@Composable
fun FxResultScreen(receipt: ExchangeReceiptArgs?, onBackToExchange: () -> Unit, modifier: Modifier = Modifier) {
    // Atrás hace lo mismo que Volver a Cambio: no se vuelve a una confirmación ya resuelta.
    BackHandler(onBack = onBackToExchange)
    ReportContentVisible(mark = FxTestTags.MARK_RESULT)
    FxResultContent(receipt = receipt, onBackToExchange = onBackToExchange, modifier = modifier)
}
