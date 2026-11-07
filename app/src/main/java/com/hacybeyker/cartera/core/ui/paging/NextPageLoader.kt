package com.hacybeyker.cartera.core.ui.paging

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Pide las páginas siguientes de una [PagedList] que vive dentro del estado de un ViewModel. [current] lee la lista
 * y [update] la reemplaza; la primera página la carga el ViewModel, junto con lo que necesite su pantalla.
 */
class NextPageLoader<T>(
    private val scope: CoroutineScope,
    private val current: () -> PagedList<T>,
    private val update: ((PagedList<T>) -> PagedList<T>) -> Unit,
    private val fetch: suspend (cursor: String) -> Outcome<Page<T>>
) {
    fun loadMore() {
        if (current().canLoadMore) load()
    }

    fun retry() {
        val list = current()
        if (list.loadMoreError != null && !list.isLoadingMore) load()
    }

    private fun load() {
        val cursor = current().nextCursor ?: return
        update { it.loadingMore() }
        scope.launch {
            val outcome = fetch(cursor)
            update { list ->
                when (outcome) {
                    is Outcome.Success -> list.appended(outcome.value)
                    is Outcome.Failure -> list.loadMoreFailed(outcome.error)
                }
            }
        }
    }
}
