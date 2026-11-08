package com.hacybeyker.cartera.legacy.pin.domain

const val PIN_PAD_COLUMNS = 3
const val PIN_PAD_ROWS = 4

private const val DIGITS = 10
private const val LAST_DIGIT_SLOT = 9
private const val DELETE_SLOT = 10

/** Un rectángulo en píxeles de la vista: incluye `left` y `top`, excluye `right` y `bottom`. */
data class PinBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun contains(x: Int, y: Int): Boolean = x in left until right && y in top until bottom
}

/** Una tecla con su lugar en la cuadrícula. El [id] es su posición en el orden de lectura. */
data class PinSlot(val id: Int, val key: PinKey, val row: Int, val column: Int)

/**
 * La cuadrícula de 3 columnas por 4 filas: tres filas de tres dígitos y, abajo, un hueco, el último dígito y Borrar.
 * Los toques y los clics de accesibilidad se resuelven con esta misma geometría.
 */
class PinPadLayout(order: List<Int>) {

    val slots: List<PinSlot>

    init {
        require(order.size == DIGITS && order.toSet() == (0 until DIGITS).toSet()) {
            "El teclado lleva los diez dígitos, una vez cada uno"
        }
        val digits = order.mapIndexed { index, digit ->
            val cell = if (index < LAST_DIGIT_SLOT) index else PIN_PAD_COLUMNS * (PIN_PAD_ROWS - 1) + 1
            PinSlot(index, PinKey.Digit(digit), cell / PIN_PAD_COLUMNS, cell % PIN_PAD_COLUMNS)
        }
        slots = digits + PinSlot(DELETE_SLOT, PinKey.Delete, PIN_PAD_ROWS - 1, PIN_PAD_COLUMNS - 1)
    }

    fun slot(id: Int): PinSlot? = slots.getOrNull(id)

    fun boundsOf(slot: PinSlot, width: Int, height: Int): PinBounds = PinBounds(
        left = slot.column * width / PIN_PAD_COLUMNS,
        top = slot.row * height / PIN_PAD_ROWS,
        right = (slot.column + 1) * width / PIN_PAD_COLUMNS,
        bottom = (slot.row + 1) * height / PIN_PAD_ROWS
    )

    /** La tecla que cubre el punto ([x], [y]) de una vista de [width] por [height], o `null` si cae en el hueco. */
    fun slotAt(x: Int, y: Int, width: Int, height: Int): PinSlot? =
        slots.firstOrNull { boundsOf(it, width, height).contains(x, y) }
}
