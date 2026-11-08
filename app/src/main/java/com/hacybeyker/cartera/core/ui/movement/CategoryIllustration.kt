package com.hacybeyker.cartera.core.ui.movement

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.domain.movement.MovementCategory

private const val GLYPH_FRACTION = 0.55f

/**
 * Ilustración de la categoría: un vector que se dibuja al tamaño en que se muestra, sin decodificar ningún bitmap.
 */
@Composable
fun CategoryIllustration(category: MovementCategory, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = stringResource(category.labelRes()),
            modifier = Modifier.size(size * GLYPH_FRACTION),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@DrawableRes
fun MovementCategory.fullIllustrationRes(): Int = when (this) {
    MovementCategory.GROCERIES, MovementCategory.SHOPPING -> R.drawable.ill_category_groceries
    MovementCategory.TRANSFER, MovementCategory.SALARY -> R.drawable.ill_category_transfer
    MovementCategory.HEALTH -> R.drawable.ill_category_health
    else -> R.drawable.ill_category_generic
}

@StringRes
fun MovementCategory.labelRes(): Int = when (this) {
    MovementCategory.GROCERIES -> R.string.category_groceries
    MovementCategory.SHOPPING -> R.string.category_shopping
    MovementCategory.RESTAURANTS -> R.string.category_restaurants
    MovementCategory.TRANSPORT -> R.string.category_transport
    MovementCategory.HEALTH -> R.string.category_health
    MovementCategory.ENTERTAINMENT -> R.string.category_entertainment
    MovementCategory.SERVICES -> R.string.category_services
    MovementCategory.SALARY -> R.string.category_salary
    MovementCategory.TRANSFER -> R.string.category_transfer
    MovementCategory.OTHER -> R.string.category_other
}

private fun MovementCategory.icon(): ImageVector = when (this) {
    MovementCategory.GROCERIES -> Icons.Filled.ShoppingCart
    MovementCategory.SHOPPING -> Icons.Filled.Star
    MovementCategory.RESTAURANTS -> Icons.Filled.Favorite
    MovementCategory.TRANSPORT -> Icons.Filled.Place
    MovementCategory.HEALTH -> Icons.Filled.AddCircle
    MovementCategory.ENTERTAINMENT -> Icons.Filled.PlayArrow
    MovementCategory.SERVICES -> Icons.Filled.Build
    MovementCategory.SALARY -> Icons.Filled.AccountBox
    MovementCategory.TRANSFER -> Icons.AutoMirrored.Filled.Send
    MovementCategory.OTHER -> Icons.Filled.Info
}
