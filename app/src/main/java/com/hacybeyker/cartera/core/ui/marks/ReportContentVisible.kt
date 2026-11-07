package com.hacybeyker.cartera.core.ui.marks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos

val LocalVisibleContentMarker = staticCompositionLocalOf { VisibleContentMarker {} }

/** Avisa una sola vez, cuando [hasContent] pasa a `true`, que la pantalla ya muestra contenido real. */
@Composable
fun ReportContentVisible(mark: String, hasContent: Boolean = true) {
    val marker = LocalVisibleContentMarker.current
    var reported by rememberSaveable(mark) { mutableStateOf(false) }
    if (hasContent && !reported) {
        LaunchedEffect(mark) {
            // Al empezar el frame siguiente, el primero con contenido ya se dibujó.
            withFrameNanos { }
            reported = true
            marker.onContentVisible(mark)
        }
    }
}
