package com.hacybeyker.cartera.core.ui.movement

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.testing.testMovement
import com.hacybeyker.cartera.core.ui.paging.PagedList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PagedMovementListTest {
    @get:Rule
    val compose = createComposeRule()

    private val tags = MovementListTags(list = "movimientos_lista", prefix = "movimientos")
    private val page = (1..20).map { testMovement(it) }
    private var loadMoreRequests = 0
    private var retries = 0
    private val clicked = mutableListOf<String>()

    private fun show(list: PagedList<Movement>) {
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                Surface {
                    PagedMovementList(
                        movements = list,
                        tags = tags,
                        onMovementClick = { clicked += it },
                        onLoadMore = { loadMoreRequests++ },
                        onRetryLoadMore = { retries++ },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    @Test
    fun `reaching the end asks for the next page once`() {
        show(PagedList(page, nextCursor = "MOV-002-0020"))
        compose.waitForIdle()
        assertEquals(0, loadMoreRequests)

        compose.onNodeWithTag(tags.list).performScrollToNode(hasTestTag(tags.item("MOV-002-0020")))
        compose.waitForIdle()

        assertEquals(1, loadMoreRequests)
    }

    @Test
    fun `the end of the list does not ask for more`() {
        show(PagedList(page, nextCursor = null))

        compose.onNodeWithTag(tags.list).performScrollToNode(hasTestTag(tags.item("MOV-002-0020")))
        compose.waitForIdle()

        assertEquals(0, loadMoreRequests)
    }

    @Test
    fun `a failed load more shows the retry row and keeps the items`() {
        show(PagedList(page, nextCursor = "MOV-002-0020", loadMoreError = AppError.Network))

        compose.onNodeWithTag(tags.list).performScrollToNode(hasTestTag(tags.retryLoadMore))
        compose.onNodeWithTag(tags.retryLoadMore).performClick()

        assertEquals(1, retries)
        assertEquals(0, loadMoreRequests)
        compose.onNodeWithTag(tags.list).performScrollToNode(hasTestTag(tags.item("MOV-002-0001")))
        compose.onNodeWithTag(tags.item("MOV-002-0001")).performClick()
        assertEquals(listOf("MOV-002-0001"), clicked)
    }
}
