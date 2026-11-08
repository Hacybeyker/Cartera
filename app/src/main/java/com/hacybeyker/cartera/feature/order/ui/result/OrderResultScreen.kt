package com.hacybeyker.cartera.feature.order.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.order.ui.OrderReceiptArgs
import com.hacybeyker.cartera.feature.order.ui.OrderTestTags

@Composable
fun OrderResultScreen(receipt: OrderReceiptArgs?, onBackToMarket: () -> Unit, modifier: Modifier = Modifier) {
    // Atrás hace lo mismo que Volver a Mercado: no se vuelve a una confirmación ya resuelta.
    BackHandler(onBack = onBackToMarket)
    ReportContentVisible(mark = OrderTestTags.MARK_RESULT)
    OrderResultContent(receipt = receipt, onBackToMarket = onBackToMarket, modifier = modifier)
}
