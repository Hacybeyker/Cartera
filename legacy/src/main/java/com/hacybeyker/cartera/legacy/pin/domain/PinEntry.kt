package com.hacybeyker.cartera.legacy.pin.domain

const val PIN_LENGTH = 4

/** Los dígitos tecleados de la clave. Solo se entrega con [take], que además los borra. */
class PinEntry {
    private val digits = StringBuilder()

    val size: Int get() = digits.length

    val isFull: Boolean get() = digits.length == PIN_LENGTH

    /** Agrega [digit] si hay lugar; devuelve si lo hizo. */
    fun append(digit: Int): Boolean {
        if (isFull || digit !in 0..9) return false
        digits.append(digit)
        return true
    }

    /** Quita el último dígito; devuelve si había alguno. */
    fun deleteLast(): Boolean {
        if (digits.isEmpty()) return false
        digits.setLength(digits.length - 1)
        return true
    }

    /** La clave tecleada, y la entrada queda vacía. */
    fun take(): String = digits.toString().also { clear() }

    fun clear() {
        digits.setLength(0)
    }
}
