package com.hacybeyker.cartera.core.testing

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.hacybeyker.cartera.core.designsystem.CarteraTheme

private const val SCREENSHOTS_DIR = "src/test/screenshots"

/**
 * Graba [name]_claro.png y [name]_oscuro.png con el tema fijo (sin color dinámico). Con [withWindows] captura
 * la pantalla entera, incluidos los diálogos, que se dibujan en otra ventana.
 */
@OptIn(ExperimentalRoborazziApi::class)
fun ComposeContentTestRule.captureLightAndDark(
    name: String,
    withWindows: Boolean = false,
    content: @Composable () -> Unit
) {
    var dark by mutableStateOf(false)
    setContent {
        CarteraTheme(darkTheme = dark, dynamicColor = false) {
            Surface { content() }
        }
    }
    // Oscuro primero: así cada captura sigue a un cambio de estado. Capturar la composición inicial sin
    // cambios salía en blanco en algunas pruebas, aunque avanzara el reloj.
    listOf(true, false).forEach { isDark ->
        dark = isDark
        waitForIdle()
        val path = "$SCREENSHOTS_DIR/${name}_${if (isDark) "oscuro" else "claro"}.png"
        if (withWindows) captureScreenRoboImage(path) else onRoot().captureRoboImage(path)
    }
}
