package com.hacybeyker.cartera.core.domain.idempotency

fun interface IdempotencyKeyFactory {
    fun newKey(): String
}
