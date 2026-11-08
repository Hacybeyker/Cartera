package com.hacybeyker.cartera.feature.movements.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.testing.pen
import com.hacybeyker.cartera.core.ui.LoadUiState
import com.hacybeyker.cartera.feature.movements.TestBreakdown
import com.hacybeyker.cartera.feature.movements.domain.model.BreakdownLine
import com.hacybeyker.cartera.feature.movements.ui.breakdown.MovementBreakdownContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovementBreakdownContentTest {
    @get:Rule
    val compose = createComposeRule()

    // Como MOV-002-0001: 40 líneas, muchas más de las que caben en pantalla.
    private val longBreakdown = TestBreakdown.copy(lines = (1..40).map { BreakdownLine("Concepto $it", pen("$it.00")) })

    private fun show(richTransition: Boolean) {
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                Surface {
                    MovementBreakdownContent(
                        uiState = LoadUiState(isLoading = false, content = longBreakdown),
                        richTransition = richTransition,
                        onAction = {},
                        onBack = {}
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the lazy list composes only the visible lines`() {
        show(richTransition = false)

        compose.onNodeWithTag(MovementBreakdownTestTags.ILLUSTRATION).assertExists()
        compose.onNodeWithTag(MovementBreakdownTestTags.line(1)).assertExists()
        compose.onNodeWithTag(MovementBreakdownTestTags.line(40)).assertDoesNotExist()
    }

    @Test
    fun `the rich transition composes every line at once`() {
        show(richTransition = true)

        compose.onNodeWithTag(MovementBreakdownTestTags.ILLUSTRATION).assertExists()
        compose.onNodeWithTag(MovementBreakdownTestTags.line(1)).assertExists()
        compose.onNodeWithTag(MovementBreakdownTestTags.line(40)).assertExists()
    }
}
