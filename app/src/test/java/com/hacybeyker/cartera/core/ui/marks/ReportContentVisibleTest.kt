package com.hacybeyker.cartera.core.ui.marks

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportContentVisibleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `reports once, only after there is content`() {
        val marks = mutableListOf<String>()
        var hasContent by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalVisibleContentMarker provides VisibleContentMarker { marks += it }) {
                ReportContentVisible(mark = "login", hasContent = hasContent)
            }
        }
        compose.waitForIdle()
        assertEquals(emptyList<String>(), marks)

        hasContent = true
        compose.waitForIdle()
        hasContent = false
        compose.waitForIdle()
        hasContent = true
        compose.waitForIdle()

        assertEquals(listOf("login"), marks)
    }
}
