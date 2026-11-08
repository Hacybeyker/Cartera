package com.hacybeyker.cartera.feature.order.ui.confirm

import com.hacybeyker.cartera.feature.order.ui.OrderReceiptArgs

/** Adónde sale la confirmación según cómo termina. */
class OrderConfirmNavigation(
    val onBack: () -> Unit,
    val onResult: (OrderReceiptArgs?) -> Unit,
    val onCancelled: () -> Unit
)
