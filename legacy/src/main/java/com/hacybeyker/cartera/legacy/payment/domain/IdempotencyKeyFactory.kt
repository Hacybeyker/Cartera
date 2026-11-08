package com.hacybeyker.cartera.legacy.payment.domain

fun interface IdempotencyKeyFactory {
    /** Una clave de idempotencia nueva: cada intento de clave de operaciones lleva la suya. */
    fun create(): String
}
