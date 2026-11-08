package com.hacybeyker.cartera.legacy.payment.data

import com.hacybeyker.cartera.legacy.payment.domain.IdempotencyKeyFactory
import java.util.UUID
import javax.inject.Inject

class UuidIdempotencyKeyFactory @Inject constructor() : IdempotencyKeyFactory {
    override fun create(): String = UUID.randomUUID().toString()
}
