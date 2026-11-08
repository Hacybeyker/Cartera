package com.hacybeyker.cartera.core.data.idempotency

import com.hacybeyker.cartera.core.domain.idempotency.IdempotencyKeyFactory
import java.util.UUID
import javax.inject.Inject

class UuidIdempotencyKeyFactory @Inject constructor() : IdempotencyKeyFactory {
    override fun newKey(): String = UUID.randomUUID().toString()
}
