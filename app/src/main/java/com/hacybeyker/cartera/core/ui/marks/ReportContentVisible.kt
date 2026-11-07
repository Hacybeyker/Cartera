package com.hacybeyker.cartera.core.ui.marks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

val LocalVisibleContentMarker = staticCompositionLocalOf { VisibleContentMarker {} }

/** Lo que es igual en cada composición: pantallas sin carga, que avisan una sola vez. */
const val STATIC_CONTENT = "static"

/**
 * Avisa [mark] tras el primer frame que muestra el contenido identificado por [contentKey]: una vez por cada clave
 * nueva (una carga o un refresco), nunca dos por la misma. `null` significa que todavía no hay contenido real.
 */
@Composable
fun ReportContentVisible(mark: String, contentKey: Any? = STATIC_CONTENT) {
    if (contentKey == null) return
    val marker = LocalVisibleContentMarker.current
    val reported = reportedContent()
    LaunchedEffect(mark, contentKey) {
        if (reported.lastKeys[mark] == contentKey) return@LaunchedEffect
        // Al empezar el frame siguiente, el primero con este contenido ya se dibujó.
        withFrameNanos { }
        reported.lastKeys[mark] = contentKey
        marker.onContentVisible(mark)
    }
}

// Vive en el ViewModelStore de la entrada, igual que el ViewModel que produce las claves: una rotación o volver a
// la entrada no repite el aviso, y una entrada nueva o la muerte del proceso empiezan de cero, como su contenido.
internal class ReportedContent : ViewModel() {
    val lastKeys = mutableMapOf<String, Any>()
}

@Composable
private fun reportedContent(): ReportedContent {
    val owner = LocalViewModelStoreOwner.current ?: return remember { ReportedContent() }
    return viewModel(viewModelStoreOwner = owner, key = ReportedContent::class.java.name)
}
