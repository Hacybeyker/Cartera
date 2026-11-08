package com.hacybeyker.cartera.feature.transfer

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.pen
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferReceipt
import com.hacybeyker.cartera.feature.transfer.domain.repository.IdempotencyKeyFactory
import com.hacybeyker.cartera.feature.transfer.domain.repository.TransferRepository

/** La transferencia de referencia: ACC-002 → 999-0000001-0-01 por S/ 150.00. */
val ReferenceOrder = TransferOrder(
    sourceAccountId = "ACC-002",
    sourceAlias = "Cuenta Gastos",
    sourceNumberMasked = "****9034",
    destinationAccountNumber = "999-0000001-0-01",
    amount = pen("150.00"),
    description = null,
    idempotencyKey = "key-1"
)

val ReferenceReceipt = TransferReceipt(
    transferId = "TRF-000001",
    operationNumber = "90000001",
    sourceAccountId = "ACC-002",
    sourceAvailableBalance = pen("2150.00"),
    movementId = "MOV-002-T0001"
)

class FakeTransferRepository : TransferRepository {
    /** Errores para los próximos envíos, en orden; vacía, la transferencia sale bien. */
    val failures = ArrayDeque<AppError>()
    val sent = mutableListOf<TransferOrder>()

    override suspend fun transfer(order: TransferOrder): Outcome<TransferReceipt> {
        sent += order
        return failures.removeFirstOrNull()?.let { Outcome.Failure(it) } ?: Outcome.Success(ReferenceReceipt)
    }
}

class SequentialKeys : IdempotencyKeyFactory {
    private var next = 1

    override fun newKey(): String = "key-${next++}"
}
