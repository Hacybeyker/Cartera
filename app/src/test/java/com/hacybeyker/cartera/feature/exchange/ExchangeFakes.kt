package com.hacybeyker.cartera.feature.exchange

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.exchange.ExchangeAccount
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.core.testing.TestFxQuote
import com.hacybeyker.cartera.feature.exchange.domain.model.ExchangeReceipt
import com.hacybeyker.cartera.feature.exchange.domain.repository.ExchangeRepository
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred

fun usd(value: String) = Money(BigDecimal(value), "USD")

fun pen(value: String) = Money(BigDecimal(value), "PEN")

/** El cambio de referencia: US$ 100.00 de ACC-003 a ACC-002, con la cotización inicial (vence a las 10:00:30). */
val ReferenceExchange = ExchangeOrder(
    direction = ExchangeDirection.USD_TO_PEN,
    quoteId = TestFxQuote.quoteId,
    rate = TestFxQuote.buy,
    expiresAt = TestFxQuote.expiresAt,
    source = ExchangeAccount("ACC-003", "Cuenta Dólares", "****1177", "USD"),
    destination = ExchangeAccount("ACC-002", "Cuenta Gastos", "****9034", "PEN"),
    amount = usd("100.00"),
    idempotencyKey = "key-0"
)

val ReferenceExchangeReceipt = ExchangeReceipt(
    exchangeId = "FX-000001",
    operationNumber = "80000001",
    quoteId = TestFxQuote.quoteId,
    rate = BigDecimal("3.742"),
    debited = usd("100.00"),
    credited = pen("374.20"),
    sourceAccountId = "ACC-003",
    sourceAvailableBalance = usd("950.20"),
    destinationAccountId = "ACC-002",
    destinationAvailableBalance = pen("2674.20"),
    movementIds = listOf("MOV-003-X0001", "MOV-002-X0001")
)

class FakeExchangeRepository : ExchangeRepository {
    /** Errores para los próximos envíos, en orden; vacía, el cambio sale bien. */
    val failures = ArrayDeque<AppError>()
    val sent = mutableListOf<ExchangeOrder>()

    /** Si no es null, cada envío espera a que se complete: deja ver el progreso. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun exchange(order: ExchangeOrder): Outcome<ExchangeReceipt> {
        sent += order
        gate?.await()
        return failures.removeFirstOrNull()?.let { Outcome.Failure(it) }
            ?: Outcome.Success(ReferenceExchangeReceipt.copy(quoteId = order.quoteId))
    }
}
