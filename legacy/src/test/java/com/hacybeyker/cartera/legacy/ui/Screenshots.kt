package com.hacybeyker.cartera.legacy.ui

import android.app.Activity
import com.github.takahirom.roborazzi.captureRoboImage
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment

private const val SCREENSHOTS_DIR = "src/test/screenshots"

/**
 * Graba [name]_claro.png y [name]_oscuro.png de la ventana de [activity]. Se crea de nuevo en cada modo porque los
 * recursos se resuelven al crearla.
 */
fun captureLightAndDark(name: String, activity: Class<out Activity>) {
    listOf(true, false).forEach { dark ->
        RuntimeEnvironment.setQualifiers(if (dark) "+night" else "+notnight")
        val controller = Robolectric.buildActivity(activity).setup()
        val suffix = if (dark) "oscuro" else "claro"
        controller.get().window.decorView.captureRoboImage("$SCREENSHOTS_DIR/${name}_$suffix.png")
        controller.close()
    }
}
