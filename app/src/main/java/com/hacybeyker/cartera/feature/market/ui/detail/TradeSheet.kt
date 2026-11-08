package com.hacybeyker.cartera.feature.market.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.component.windowRootTestTag
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.market.OrderSide
import kotlinx.coroutines.launch

/**
 * Comprar o Vender [symbol]. Sin tenencia, Vender queda deshabilitado con un texto que lo explica. Elegir cierra el
 * sheet con su animación y después avisa con [onChoose].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TradeSheet(symbol: String, canSell: Boolean, onChoose: (OrderSide) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val choose: (OrderSide) -> Unit = { side ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { onChoose(side) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // El sheet se dibuja en otra ventana, como los diálogos.
        modifier = Modifier.windowRootTestTag(StockDetailTestTags.TRADE_SHEET),
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.spacing.screen, end = MaterialTheme.spacing.screen)
                .padding(bottom = MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.stock_trade_sheet_title, symbol),
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = { choose(OrderSide.BUY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(StockDetailTestTags.TRADE_BUY)
            ) {
                Text(text = stringResource(R.string.stock_trade_buy))
            }
            OutlinedButton(
                onClick = { choose(OrderSide.SELL) },
                enabled = canSell,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(StockDetailTestTags.TRADE_SELL)
            ) {
                Text(text = stringResource(R.string.stock_trade_sell))
            }
            if (!canSell) {
                Text(
                    text = stringResource(R.string.stock_trade_sell_unavailable, symbol),
                    modifier = Modifier.testTag(StockDetailTestTags.TRADE_SELL_UNAVAILABLE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
