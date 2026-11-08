package com.hacybeyker.cartera.legacy.pin.domain

/** El teclado de la clave: lo tecleado y el orden de sus dígitos, que cambia con cada clave rechazada. */
class PinKeyboard {
    private val entry = PinEntry()
    private var seed = 0
    private var rejections = 0

    val order: List<Int> get() = PinPadOrder.afterRejections(seed, rejections)

    val filled: Int get() = entry.size

    val isFull: Boolean get() = entry.isFull

    fun start(seed: Int) {
        this.seed = seed
    }

    fun append(digit: Int): Boolean = entry.append(digit)

    fun deleteLast(): Boolean = entry.deleteLast()

    /** La clave tecleada, y la entrada queda vacía. */
    fun takePin(): String = entry.take()

    /** El servidor rechazó la clave: se descarta lo tecleado y el teclado cambia de orden. */
    fun rejectPin() {
        rejections++
        entry.clear()
    }

    fun clear() = entry.clear()
}
