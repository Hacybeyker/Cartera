package com.hacybeyker.cartera.feature.transfer.domain.repository

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferReceipt

interface TransferRepository {
    suspend fun transfer(order: TransferOrder): Outcome<TransferReceipt>
}
