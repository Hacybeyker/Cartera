package com.hacybeyker.cartera.feature.home.ui.menu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.spacing

/** Contenido del menú lateral de Home. */
@Composable
fun HomeMenu(onItemClick: (HomeMenuItem) -> Unit, modifier: Modifier = Modifier) {
    ModalDrawerSheet(modifier = modifier.testTag(HomeMenuTestTags.CONTAINER)) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            style = MaterialTheme.typography.titleLarge
        )
        HomeMenuItem.entries.forEach { item ->
            NavigationDrawerItem(
                label = { Text(text = stringResource(item.label)) },
                selected = false,
                onClick = { onItemClick(item) },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag(item.testTag),
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                badge = if (item.isComingSoon) {
                    { Text(text = stringResource(R.string.common_coming_soon)) }
                } else {
                    null
                }
            )
        }
    }
}
