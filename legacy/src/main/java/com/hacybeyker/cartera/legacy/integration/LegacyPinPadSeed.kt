package com.hacybeyker.cartera.legacy.integration

fun interface LegacyPinPadSeed {
    /** La semilla del orden de los dígitos del teclado de la clave: 0 deja el orden fijo, 1 al 9 y luego 0. */
    fun current(): Int
}
