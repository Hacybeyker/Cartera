package com.hacybeyker.cartera.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.feature.home.ui.HomeNavKey

/** Deja la pila solo con [key]. Agrega antes de quitar para que NavDisplay nunca vea la pila vacía. */
fun NavBackStack<NavKey>.resetTo(key: NavKey) {
    add(key)
    while (size > 1) removeAt(0)
}

/** Vuelve hasta [key], que queda arriba; si no está en la pila, la deja solo con [key]. */
fun NavBackStack<NavKey>.popTo(key: NavKey) {
    if (key !in this) return resetTo(key)
    while (last() != key) removeAt(lastIndex)
}

/** Vuelve hasta [key] si está en la pila; si no (no debería pasar), vuelve a Home. */
fun NavBackStack<NavKey>.popToOrHome(key: NavKey) {
    if (key in this) popTo(key) else popTo(HomeNavKey)
}
