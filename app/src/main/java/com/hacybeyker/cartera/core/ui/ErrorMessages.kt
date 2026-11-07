package com.hacybeyker.cartera.core.ui

import androidx.annotation.StringRes
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.domain.AppError

@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    AppError.Server -> R.string.error_server
    AppError.InvalidCredentials -> R.string.login_invalid_credentials
    AppError.SessionExpired -> R.string.session_expired_message
    AppError.NotFound -> R.string.error_not_found
    is AppError.Validation -> R.string.error_validation
}

/** Reintentar no arregla un id inexistente: esos errores se muestran sin botón. */
val AppError.isRetryable: Boolean get() = this != AppError.NotFound
