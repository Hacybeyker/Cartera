package com.hacybeyker.cartera.legacy.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.core.domain.LegacyError

/** El diálogo de AppCompat con Reintentar y Cancelar que muestran las pantallas ante un error de red o del servidor. */
internal class ErrorDialog(
    private val context: Context,
    private val onRetry: () -> Unit,
    private val onCancel: () -> Unit
) {
    private var dialog: AlertDialog? = null

    /** Lo muestra mientras haya [error] y lo cierra cuando ya no hay. */
    fun show(error: LegacyError?) {
        if (error == null) {
            dismiss()
        } else if (dialog?.isShowing != true) {
            dialog = AlertDialog.Builder(context)
                .setTitle(R.string.legacy_error_title)
                .setMessage(error.messageRes())
                .setCancelable(false)
                .setPositiveButton(R.string.legacy_error_retry) { _, _ -> onRetry() }
                .setNegativeButton(R.string.legacy_error_cancel) { _, _ -> onCancel() }
                .show()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    @StringRes
    private fun LegacyError.messageRes(): Int = when (this) {
        LegacyError.Network -> R.string.legacy_error_network
        else -> R.string.legacy_error_server
    }
}
