package com.hacybeyker.cartera.core.domain.pinpad

/** Semilla del orden de los dígitos del teclado de la clave. 0 deja el orden fijo: 1 al 9 y luego 0. */
fun interface PinPadSeed {
    fun current(): Int
}
