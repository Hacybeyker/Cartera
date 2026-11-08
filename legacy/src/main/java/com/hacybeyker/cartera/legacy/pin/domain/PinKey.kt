package com.hacybeyker.cartera.legacy.pin.domain

/** Lo que una tecla del teclado de la clave le pide a la pantalla. */
sealed interface PinKey {
    data class Digit(val value: Int) : PinKey

    data object Delete : PinKey
}
