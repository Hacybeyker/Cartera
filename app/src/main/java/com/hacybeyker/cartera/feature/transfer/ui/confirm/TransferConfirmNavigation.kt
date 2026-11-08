package com.hacybeyker.cartera.feature.transfer.ui.confirm

import com.hacybeyker.cartera.feature.transfer.ui.TransferReceiptArgs

/** Adónde sale la confirmación según cómo termina. */
class TransferConfirmNavigation(
    val onBack: () -> Unit,
    val onResult: (TransferReceiptArgs?) -> Unit,
    val onCancelled: () -> Unit
)
