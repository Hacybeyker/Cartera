package com.hacybeyker.cartera.core.ui.movement

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.format.formattedSigned
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.paging.PagedList
import kotlinx.coroutines.flow.first

// Se pide la página siguiente cuando faltan estas filas por mostrar, para que llegue antes del final.
private const val PREFETCH_DISTANCE = 5

/** testTags de una lista de movimientos: `<lista>`, `<prefijo>_item_<id>`, `<prefijo>_mas_cargando` y `_mas_reintentar`. */
class MovementListTags(val list: String, prefix: String) {
    private val item = "${prefix}_item_"
    val loadingMore = "${prefix}_mas_cargando"
    val retryLoadMore = "${prefix}_mas_reintentar"

    fun item(id: String) = item + id
}

/**
 * Lista paginada de movimientos. Pide la página siguiente al acercarse al final; si esa carga falla, muestra una
 * fila con Reintentar y mantiene lo ya cargado.
 */
@Composable
fun PagedMovementList(
    movements: PagedList<Movement>,
    tags: MovementListTags,
    onMovementClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    header: LazyListScope.() -> Unit = {}
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(state, movements.items.size, movements.canLoadMore) {
        if (!movements.canLoadMore) return@LaunchedEffect
        snapshotFlow {
            // Antes del primer layout no hay filas visibles: sin este cuidado, se pediría al abrir la pantalla.
            val layout = state.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            lastVisible >= layout.totalItemsCount - PREFETCH_DISTANCE
        }.first { it }
        currentOnLoadMore()
    }
    LazyColumn(modifier = modifier.testTag(tags.list), state = state) {
        header()
        items(items = movements.items, key = { it.id }) { movement ->
            MovementRow(
                movement = movement,
                onClick = { onMovementClick(movement.id) },
                modifier = Modifier.testTag(tags.item(movement.id))
            )
            HorizontalDivider()
        }
        loadMoreFooter(movements = movements, tags = tags, onRetry = onRetryLoadMore)
    }
}

private fun LazyListScope.loadMoreFooter(movements: PagedList<Movement>, tags: MovementListTags, onRetry: () -> Unit) {
    val error = movements.loadMoreError
    when {
        error != null -> item(key = tags.retryLoadMore) { LoadMoreError(message = error.messageRes(), tags, onRetry) }
        movements.isLoadingMore -> item(key = tags.loadingMore) { LoadingMore(tags) }
        else -> Unit
    }
}

@Composable
private fun LoadingMore(tags: MovementListTags) {
    val description = stringResource(R.string.common_loading)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.md)
            .testTag(tags.loadingMore)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadMoreError(@StringRes message: Int, tags: MovementListTags, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(message), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onRetry, modifier = Modifier.testTag(tags.retryLoadMore)) {
            Text(text = stringResource(R.string.movements_load_more_retry))
        }
    }
}

@Composable
fun MovementRow(movement: Movement, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.screen, vertical = MaterialTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIllustration(category = movement.category, size = MaterialTheme.sizes.listIcon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movement.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = movement.date.formatted(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = movement.amount.formattedSigned(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (movement.amount.amount.signum() > 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
