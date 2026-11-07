package com.hacybeyker.cartera.core.ui.marks

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportContentVisibleTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val marks = mutableListOf<String>()
    private var contentKey: Int? by mutableStateOf(null)
    private var isShown by mutableStateOf(true)

    @Composable
    private fun Host() {
        CompositionLocalProvider(LocalVisibleContentMarker provides VisibleContentMarker { marks += it }) {
            if (isShown) ReportContentVisible(mark = "inicio_cuentas", contentKey = contentKey)
        }
    }

    @Test
    fun `reports once per content, only after there is content`() {
        compose.setContent { Host() }
        compose.waitForIdle()
        assertEquals(emptyList<String>(), marks)

        contentKey = 1
        compose.waitForIdle()
        contentKey = null
        compose.waitForIdle()
        contentKey = 1
        compose.waitForIdle()

        assertEquals(listOf("inicio_cuentas"), marks)
    }

    @Test
    fun `coming back with refreshed content reports again`() {
        contentKey = 1
        compose.setContent { Host() }
        compose.waitForIdle()

        // Se apila otra pantalla encima (la entrada sale de la composición) y al volver hay un refresco.
        isShown = false
        compose.waitForIdle()
        contentKey = 2
        isShown = true
        compose.waitForIdle()

        assertEquals(listOf("inicio_cuentas", "inicio_cuentas"), marks)
    }

    @Test
    fun `coming back without new content does not report again`() {
        contentKey = 1
        compose.setContent { Host() }
        compose.waitForIdle()

        isShown = false
        compose.waitForIdle()
        isShown = true
        compose.waitForIdle()

        assertEquals(listOf("inicio_cuentas"), marks)
    }

    @Test
    fun `rotation without new content does not report again`() {
        contentKey = 1
        compose.setContent { Host() }
        compose.waitForIdle()

        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity { activity -> activity.setContent { Host() } }
        compose.waitForIdle()

        assertEquals(listOf("inicio_cuentas"), marks)
    }

    @Test
    fun `static screens report once`() {
        compose.setContent {
            CompositionLocalProvider(LocalVisibleContentMarker provides VisibleContentMarker { marks += it }) {
                ReportContentVisible(mark = "login")
            }
        }
        compose.waitForIdle()

        assertEquals(listOf("login"), marks)
    }
}
