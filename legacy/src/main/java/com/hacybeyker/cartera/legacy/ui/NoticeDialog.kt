package com.hacybeyker.cartera.legacy.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.hacybeyker.cartera.legacy.R
import com.hacybeyker.cartera.legacy.pin.domain.PinNotice

/** El diálogo de AppCompat de un solo botón con el que la Clave avisa un rechazo del pago. */
internal class NoticeDialog(private val context: Context, private val onAccept: () -> Unit) {
    private var dialog: AlertDialog? = null

    /** Lo muestra mientras haya [notice] y lo cierra cuando ya no hay. */
    fun show(notice: PinNotice?) {
        if (notice == null) {
            dismiss()
        } else if (dialog?.isShowing != true) {
            dialog = AlertDialog.Builder(context)
                .setTitle(R.string.legacy_error_title)
                .setMessage(notice.messageRes())
                .setCancelable(false)
                .setPositiveButton(R.string.legacy_notice_accept) { _, _ -> onAccept() }
                .show()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    @StringRes
    private fun PinNotice.messageRes(): Int = when (this) {
        PinNotice.INSUFFICIENT_FUNDS -> R.string.legacy_pin_notice_funds
        PinNotice.DEBT_ALREADY_PAID -> R.string.legacy_pin_notice_paid
        PinNotice.REJECTED -> R.string.legacy_pin_notice_rejected
    }
}
