package com.hacybeyker.cartera.core.ui.marks

/**
 * Punto de extensión: recibe el nombre de la marca de cada pantalla una sola vez, tras su primer frame con
 * contenido real. En la app no hay ningún binding, así que no hace nada; quien quiera medir agrega uno al
 * `Set` con `@IntoSet` desde su propio módulo de Hilt, sin tocar las pantallas.
 */
fun interface VisibleContentMarker {
    fun onContentVisible(mark: String)
}
