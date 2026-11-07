package com.hacybeyker.cartera.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Deja la pila solo con [key]. Agrega antes de quitar para que NavDisplay nunca vea la pila vacía. */
fun NavBackStack<NavKey>.resetTo(key: NavKey) {
    add(key)
    while (size > 1) removeAt(0)
}
