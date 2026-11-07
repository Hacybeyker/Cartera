package com.hacybeyker.cartera.core.ui.paging

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedListTest {

    @Test
    fun `appends the next page and moves the cursor`() {
        val list = PagedList.of(Page(listOf(1, 2), nextCursor = "2")).loadingMore()

        val next = list.appended(Page(listOf(3), nextCursor = null))

        assertEquals(listOf(1, 2, 3), next.items)
        assertFalse(next.hasMore)
        assertFalse(next.isLoadingMore)
    }

    @Test
    fun `a failed load more keeps what was loaded and waits for a retry`() {
        val failed = PagedList.of(Page(listOf(1, 2), nextCursor = "2")).loadingMore().loadMoreFailed(AppError.Network)

        assertEquals(listOf(1, 2), failed.items)
        assertEquals(AppError.Network, failed.loadMoreError)
        assertFalse(failed.canLoadMore)
        assertEquals(null, failed.loadingMore().loadMoreError)
    }

    @Test
    fun `cannot load more at the end of the list`() {
        assertFalse(PagedList.of(Page(listOf(1), nextCursor = null)).canLoadMore)
        assertTrue(PagedList.of(Page(listOf(1), nextCursor = "1")).canLoadMore)
    }
}
