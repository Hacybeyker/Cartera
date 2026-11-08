package com.hacybeyker.cartera.feature.transfer.domain.repository

fun interface IdempotencyKeyFactory {
    fun newKey(): String
}
