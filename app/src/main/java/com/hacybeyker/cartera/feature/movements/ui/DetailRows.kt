package com.hacybeyker.cartera.feature.movements.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hacybeyker.cartera.R

// Los códigos que no conoce la app se muestran tal como llegan.
@Composable
internal fun channelLabel(channel: String): String = channelRes(channel)?.let { stringResource(it) } ?: channel

@Composable
internal fun statusLabel(status: String): String = statusRes(status)?.let { stringResource(it) } ?: status

@StringRes
private fun channelRes(channel: String): Int? = when (channel) {
    "POS" -> R.string.channel_pos
    "ONLINE" -> R.string.channel_online
    "APP" -> R.string.channel_app
    "BRANCH" -> R.string.channel_branch
    else -> null
}

@StringRes
private fun statusRes(status: String): Int? = when (status) {
    "COMPLETED" -> R.string.status_completed
    else -> null
}
