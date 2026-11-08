package com.hacybeyker.cartera.feature.transfer.data.repository

import com.hacybeyker.cartera.feature.transfer.domain.repository.IdempotencyKeyFactory
import java.util.UUID
import javax.inject.Inject

class UuidIdempotencyKeyFactory @Inject constructor() : IdempotencyKeyFactory {
    override fun newKey(): String = UUID.randomUUID().toString()
}
