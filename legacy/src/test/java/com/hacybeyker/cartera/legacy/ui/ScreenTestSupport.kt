package com.hacybeyker.cartera.legacy.ui

import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import androidx.fragment.app.Fragment
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

private const val SCREEN_WIDTH = 1080
private const val SCREEN_HEIGHT = 1920

internal fun launchScreen(
    presenters: LegacyPresenterSource,
    screen: () -> Fragment
): ActivityController<LegacyHostActivity> {
    HostEnvironment.reset(presenters, screen)
    val controller = Robolectric.buildActivity(LegacyHostActivity::class.java).setup()
    shadowOf(Looper.getMainLooper()).idle()
    layoutNow(controller.get().window.decorView)
    return controller
}

/** Fuerza el medido y el layout, que Robolectric no hace solo hasta el primer dibujo. */
internal fun layoutNow(root: View) {
    root.measure(
        MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(SCREEN_HEIGHT, MeasureSpec.EXACTLY)
    )
    root.layout(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT)
}

internal fun idle() = shadowOf(Looper.getMainLooper()).idle()
