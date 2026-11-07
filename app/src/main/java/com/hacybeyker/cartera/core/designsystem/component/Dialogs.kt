package com.hacybeyker.cartera.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.spacing

private val NotDismissible = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)

/** Progreso de una acción (login, confirmar): bloquea la pantalla hasta que termina. */
@Composable
fun BlockingProgress() {
    val description = stringResource(R.string.common_loading)
    Dialog(onDismissRequest = {}, properties = NotDismissible) {
        Surface(
            modifier = Modifier.windowRootTestTag(CommonTestTags.BLOCKING_PROGRESS),
            shape = MaterialTheme.shapes.large
        ) {
            Box(
                modifier = Modifier
                    .padding(MaterialTheme.spacing.lg)
                    .semantics { contentDescription = description }
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/** Error de una acción: Reintentar la repite y Cancelar (o tocar fuera) cierra el modal. */
@Composable
fun ErrorDialog(title: String, message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.windowRootTestTag(CommonTestTags.ERROR_DIALOG),
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onRetry, modifier = Modifier.testTag(CommonTestTags.ERROR_DIALOG_RETRY)) {
                Text(text = stringResource(R.string.common_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag(CommonTestTags.ERROR_DIALOG_CANCEL)) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.windowRootTestTag(CommonTestTags.CONFIRMATION_DIALOG),
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(CommonTestTags.CONFIRMATION_DIALOG_ACCEPT)) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag(CommonTestTags.CONFIRMATION_DIALOG_CANCEL)) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    )
}

/** Sin forma de cerrarlo salvo Ingresar: sin sesión, ninguna pantalla autenticada sirve. */
@Composable
fun SessionExpiredDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.windowRootTestTag(CommonTestTags.SESSION_EXPIRED_DIALOG),
        properties = NotDismissible,
        title = { Text(text = stringResource(R.string.session_expired_title)) },
        text = { Text(text = stringResource(R.string.session_expired_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(CommonTestTags.SESSION_EXPIRED_CONFIRM)) {
                Text(text = stringResource(R.string.session_expired_confirm))
            }
        }
    )
}
