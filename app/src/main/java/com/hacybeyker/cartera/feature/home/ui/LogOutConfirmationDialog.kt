package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.component.ConfirmationDialog

/** "¿Cerrar sesión?", el mismo desde Más y desde el menú lateral. */
@Composable
fun LogOutConfirmationDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    ConfirmationDialog(
        title = stringResource(R.string.more_log_out_title),
        message = stringResource(R.string.more_log_out_message),
        confirmLabel = stringResource(R.string.more_log_out),
        onConfirm = onConfirm,
        onCancel = onCancel
    )
}
