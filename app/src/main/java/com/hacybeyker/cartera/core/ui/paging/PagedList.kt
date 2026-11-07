package com.hacybeyker.cartera.core.ui.paging

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Page

/**
 * Lista paginada por cursor. Si falla la carga de más, lo ya cargado se mantiene y [loadMoreError] queda hasta
 * que el usuario reintenta: así el scroll no repite una carga que acaba de fallar.
 */
data class PagedList<T>(
    val items: List<T> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: AppError? = null
) {
    val hasMore: Boolean get() = nextCursor != null

    val canLoadMore: Boolean get() = hasMore && !isLoadingMore && loadMoreError == null

    fun loadingMore(): PagedList<T> = copy(isLoadingMore = true, loadMoreError = null)

    fun appended(page: Page<T>): PagedList<T> =
        copy(items = items + page.items, nextCursor = page.nextCursor, isLoadingMore = false, loadMoreError = null)

    fun loadMoreFailed(error: AppError): PagedList<T> = copy(isLoadingMore = false, loadMoreError = error)

    companion object {
        fun <T> of(page: Page<T>): PagedList<T> = PagedList(items = page.items, nextCursor = page.nextCursor)
    }
}
