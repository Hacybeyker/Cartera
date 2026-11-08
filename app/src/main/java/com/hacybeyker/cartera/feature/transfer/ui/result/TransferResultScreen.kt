package com.hacybeyker.cartera.feature.transfer.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.transfer.ui.TransferReceiptArgs
import com.hacybeyker.cartera.feature.transfer.ui.TransferTestTags

@Composable
fun TransferResultScreen(receipt: TransferReceiptArgs?, onBackToHome: () -> Unit, modifier: Modifier = Modifier) {
    // Atrás hace lo mismo que Volver al inicio: no se vuelve a una confirmación ya resuelta.
    BackHandler(onBack = onBackToHome)
    ReportContentVisible(mark = TransferTestTags.MARK_RESULT)
    TransferResultContent(receipt = receipt, onBackToHome = onBackToHome, modifier = modifier)
}
