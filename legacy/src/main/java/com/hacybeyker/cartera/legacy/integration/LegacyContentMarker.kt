package com.hacybeyker.cartera.legacy.integration

fun interface LegacyContentMarker {
    /** Avisa que la pantalla identificada por [mark] ya muestra su contenido real. */
    fun onContentVisible(mark: String)
}
