package com.hacybeyker.cartera.feature.home.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.hacybeyker.cartera.R

enum class HomeTab(@StringRes val label: Int, val icon: ImageVector, val testTag: String) {
    START(R.string.home_tab_start, Icons.Filled.Home, HomeTestTags.TAB_START),
    MARKET(R.string.home_tab_market, Icons.Filled.Star, HomeTestTags.TAB_MARKET),
    EXCHANGE(R.string.home_tab_exchange, Icons.Filled.Refresh, HomeTestTags.TAB_EXCHANGE),
    MORE(R.string.home_tab_more, Icons.Filled.MoreVert, HomeTestTags.TAB_MORE)
}
