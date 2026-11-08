package com.hacybeyker.cartera.feature.exchange.ui.confirm

import com.hacybeyker.cartera.feature.exchange.ui.ExchangeReceiptArgs

/** Adónde sale la confirmación según cómo termina. */
class FxConfirmNavigation(
    val onBack: () -> Unit,
    val onResult: (ExchangeReceiptArgs?) -> Unit,
    val onCancelled: () -> Unit
)
