package com.hacybeyker.cartera.legacy.pin.domain

import java.util.Collections
import java.util.Random

/**
 * El orden de los diez dígitos en el teclado. La semilla 0 deja el orden fijo; cualquier otra baraja los dígitos con
 * Fisher-Yates sobre `java.util.Random(semilla)`: recorre los índices del último al segundo e intercambia cada uno con
 * `nextInt(índice + 1)`. El resultado es el mismo en cada ejecución y en los tests.
 */
object PinPadOrder {
    val FIXED: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)

    fun forSeed(seed: Int): List<Int> {
        if (seed == 0) return FIXED
        val digits = FIXED.toMutableList()
        val random = Random(seed.toLong())
        for (index in digits.lastIndex downTo 1) {
            Collections.swap(digits, index, random.nextInt(index + 1))
        }
        return digits
    }

    /** Tras cada clave rechazada el teclado se reordena con `semilla + rechazos`; con la semilla 0 queda fijo. */
    fun afterRejections(seed: Int, rejections: Int): List<Int> = if (seed == 0) FIXED else forSeed(seed + rejections)
}
