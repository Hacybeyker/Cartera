package com.hacybeyker.cartera.feature.home.ui.menu

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.vector.ImageVector
import com.hacybeyker.cartera.R

/** Las entradas del menú lateral, en orden. [isComingSoon] lleva la etiqueta "Próximamente". */
enum class HomeMenuItem(
    @StringRes val label: Int,
    val icon: ImageVector,
    val testTag: String,
    val isComingSoon: Boolean = false
) {
    PORTFOLIO(R.string.menu_portfolio, Icons.Filled.AccountBox, HomeMenuTestTags.PORTFOLIO),
    EXCHANGE(R.string.home_tab_exchange, Icons.Filled.Refresh, HomeMenuTestTags.EXCHANGE),
    SECURITY(R.string.more_security, Icons.Filled.Lock, HomeMenuTestTags.SECURITY),
    HELP(R.string.menu_help, Icons.Filled.Info, HomeMenuTestTags.HELP, isComingSoon = true),
    LOG_OUT(R.string.more_log_out, Icons.AutoMirrored.Filled.ExitToApp, HomeMenuTestTags.LOG_OUT)
}
