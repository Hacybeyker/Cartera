package com.hacybeyker.cartera.feature.order

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.testing.TestAccounts
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderHolding
import com.hacybeyker.cartera.feature.order.domain.model.OrderReceipt
import com.hacybeyker.cartera.feature.order.domain.repository.OrderRepository

fun usd(value: String) = Money(value.toBigDecimal(), "USD")

val DollarAccount = Account("ACC-003", "Cuenta Dólares", "SAVINGS", "USD", "****1177", usd("1050.20"), usd("1050.20"))

/** Las cuentas de Ana: dos en soles y una en dólares. */
val OrderTestAccounts = TestAccounts + DollarAccount

/** La compra de referencia: 2 ANDM a US$ 42.10 desde ACC-003. */
val ReferenceBuy = MarketOrder(
    symbol = "ANDM",
    side = OrderSide.BUY,
    quantity = 2,
    accountId = "ACC-003",
    accountAlias = "Cuenta Dólares",
    accountNumberMasked = "****1177",
    referencePrice = usd("42.10"),
    idempotencyKey = "key-1"
)

val ReferenceBuyReceipt = OrderReceipt(
    orderId = "ORD-000001",
    operationNumber = "70000001",
    side = OrderSide.BUY,
    symbol = "ANDM",
    quantity = 2,
    executedPrice = usd("42.10"),
    total = usd("84.20"),
    accountId = "ACC-003",
    accountAvailableBalance = usd("966.00"),
    movementId = "MOV-003-O0001",
    holding = OrderHolding(quantity = 12, averagePrice = usd("40.35"))
)

class FakeOrderRepository : OrderRepository {
    /** Errores para los próximos envíos, en orden; vacía, la orden sale bien. */
    val failures = ArrayDeque<AppError>()
    val sent = mutableListOf<MarketOrder>()

    override suspend fun place(order: MarketOrder): Outcome<OrderReceipt> {
        sent += order
        return failures.removeFirstOrNull()?.let { Outcome.Failure(it) } ?: Outcome.Success(ReferenceBuyReceipt)
    }
}
