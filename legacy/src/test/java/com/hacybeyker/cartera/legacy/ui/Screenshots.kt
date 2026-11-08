package com.hacybeyker.cartera.legacy.ui

import android.animation.ValueAnimator
import android.os.Looper
import androidx.fragment.app.Fragment
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.Duration
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

private const val SCREENSHOTS_DIR = "src/test/screenshots"

/**
 * Graba [name]_claro.png o [name]_oscuro.png de la ventana del anfitrión con [screen], según [dark]. Cada modo corre
 * en su propio test: dos capturas seguidas en el mismo test salían distintas de una corrida a otra.
 */
internal fun captureScreen(
    name: String,
    dark: Boolean,
    presenters: () -> LegacyPresenterSource,
    screen: () -> Fragment
) {
    RuntimeEnvironment.setQualifiers(if (dark) "+night" else "+notnight")
    HostEnvironment.reset(presenters(), screen)
    val controller = Robolectric.buildActivity(LegacyHostActivity::class.java).setup()
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    val suffix = if (dark) "oscuro" else "claro"
    controller.get().window.decorView.captureRoboImage("$SCREENSHOTS_DIR/${name}_$suffix.png")
    controller.close()
}

/** Sin animaciones, cada captura sale en su estado final. */
internal fun setAnimationsEnabled(enabled: Boolean) {
    // setDurationScale no es API pública: Robolectric ejecuta el framework real, así que se llama por reflexión.
    ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType)
        .invoke(null, if (enabled) 1f else 0f)
}
