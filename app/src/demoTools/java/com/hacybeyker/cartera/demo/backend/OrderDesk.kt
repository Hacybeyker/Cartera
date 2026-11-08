package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

enum class OrderSide { BUY, SELL }

data class MarketOrder(val symbol: String, val side: OrderSide, val quantity: Int, val accountId: String)

/** Órdenes a mercado, por cantidad entera, al precio vigente del servidor y en la cuenta en dólares. */
class OrderDesk(private val state: BackendState, private val clock: Clock) {

    fun order(userId: String, idempotencyKey: String?, body: JsonObject): BackendResponse {
        if (idempotencyKey.isNullOrBlank()) {
            reject(FieldError(TransferDesk.IDEMPOTENCY_HEADER, "REQUIRED", "La cabecera es obligatoria."))
        }
        return state.idempotency.once("$OPERATION:$userId:$idempotencyKey") { execute(userId, parse(body)) }
    }

    private fun execute(userId: String, order: MarketOrder): BackendResponse {
        val account = state.account(userId, order.accountId)
            ?: reject(FieldError("accountId", "NOT_OWNED", "La cuenta no es tuya."))
        if (account.string("currency") != MarketDesk.CURRENCY) {
            reject(FieldError("accountId", "UNSUPPORTED_CURRENCY", "Las acciones se pagan con una cuenta en dólares."))
        }
        val price = state.market.price(order.symbol)
        val total = cents(price * order.quantity.toBigDecimal())
        val available = account.availableBalance()
        val held = state.holdings.holding(userId, order.symbol)
        val heldQuantity = held?.quantity ?: 0
        val (newBalance, holding) = when (order.side) {
            OrderSide.BUY -> {
                if (total > available) reject(ProblemCode.INSUFFICIENT_FUNDS, "El total supera el saldo disponible.")
                available - total to bought(order, held, price)
            }
            OrderSide.SELL -> {
                if (order.quantity > heldQuantity) {
                    reject(ProblemCode.INSUFFICIENT_HOLDINGS, "Tienes $heldQuantity acciones de ${order.symbol}.")
                }
                available + total to Holding(order.symbol, heldQuantity - order.quantity, held?.averagePrice ?: price)
            }
        }
        state.holdings.put(userId, holding)
        return executed(order, Execution(price, total, newBalance, holding))
    }

    private fun bought(order: MarketOrder, held: Holding?, price: BigDecimal): Holding {
        val heldQuantity = held?.quantity ?: 0
        val quantity = heldQuantity + order.quantity
        val cost = (held?.averagePrice ?: BigDecimal.ZERO) * heldQuantity.toBigDecimal() +
            price * order.quantity.toBigDecimal()
        return Holding(order.symbol, quantity, cost.divide(quantity.toBigDecimal(), 2, RoundingMode.HALF_UP))
    }

    private data class Execution(
        val price: BigDecimal,
        val total: BigDecimal,
        val newBalance: BigDecimal,
        val holding: Holding
    )

    private fun executed(order: MarketOrder, execution: Execution): BackendResponse {
        val sequence = state.nextSequence(OPERATION)
        val createdAt = timestamp(clock.instant())
        val movementId = "MOV-${order.accountId.substringAfter("ACC-")}-O%04d".format(sequence)
        val operationNumber = "7%07d".format(sequence)
        val movement = movementJson(order, execution, movementId, operationNumber, createdAt)
        state.post(order.accountId, execution.newBalance, MovementRecord.from(movement))
        return json(
            status = CREATED,
            body = buildJsonObject {
                put("orderId", "ORD-%06d".format(sequence))
                put("operationNumber", operationNumber)
                put("createdAt", createdAt)
                put("side", order.side.name)
                put("symbol", order.symbol)
                put("quantity", order.quantity)
                put("executedPrice", money(execution.price, MarketDesk.CURRENCY))
                put("total", money(execution.total, MarketDesk.CURRENCY))
                put("accountId", order.accountId)
                put("accountAvailableBalance", money(execution.newBalance, MarketDesk.CURRENCY))
                put("movementId", movementId)
                putJsonObject("holding") {
                    put("symbol", order.symbol)
                    put("quantity", execution.holding.quantity)
                    put(
                        "averagePrice",
                        if (execution.holding.quantity > 0) {
                            money(execution.holding.averagePrice, MarketDesk.CURRENCY)
                        } else {
                            JsonNull
                        }
                    )
                }
            }
        )
    }

    private fun movementJson(
        order: MarketOrder,
        execution: Execution,
        movementId: String,
        operationNumber: String,
        createdAt: String
    ): JsonObject {
        val buy = order.side == OrderSide.BUY
        val name = requireNotNull(state.market.stock(order.symbol)).name
        return buildJsonObject {
            put("id", movementId)
            put("accountId", order.accountId)
            put("date", createdAt)
            put("description", "${if (buy) "Compra" else "Venta"} de ${order.quantity} ${order.symbol}")
            put("category", CATEGORY)
            put("amount", money(if (buy) execution.total.negate() else execution.total, MarketDesk.CURRENCY))
            put("type", if (buy) "DEBIT" else "CREDIT")
            put("operationNumber", operationNumber)
            put("channel", "APP")
            put("status", "COMPLETED")
            putJsonObject("counterparty") {
                put("name", name)
                put("reference", order.symbol)
            }
            putJsonObject("breakdown") {
                putJsonArray("lines") {
                    addJsonObject {
                        put("concept", "${order.quantity} ${order.symbol} × ${execution.price.toPlainString()}")
                        put("amount", money(execution.total, MarketDesk.CURRENCY))
                    }
                }
                put("fees", money(BigDecimal.ZERO, MarketDesk.CURRENCY))
                put("taxes", money(BigDecimal.ZERO, MarketDesk.CURRENCY))
                put("merchant", JsonNull)
                put("category", CATEGORY)
            }
        }
    }

    private fun parse(body: JsonObject): MarketOrder {
        val errors = mutableListOf<FieldError>()
        val symbol = body.text("symbol")
        val side = OrderSide.entries.firstOrNull { it.name == body.text("side") }
        val quantity = (body["quantity"] as? JsonPrimitive)?.takeUnless { it.isString }?.content?.toIntOrNull()
        val accountId = body.text("accountId")
        when {
            symbol.isNullOrBlank() -> errors += required("symbol")
            state.market.stock(symbol) == null ->
                errors += FieldError("symbol", "UNKNOWN_SYMBOL", "No existe una acción con ese símbolo.")
        }
        if (side == null) errors += FieldError("side", "INVALID_VALUE", "Usa BUY o SELL.")
        when {
            quantity == null -> errors += FieldError("quantity", "INVALID_FORMAT", "Usa un número entero.")
            quantity <= 0 ->
                errors +=
                    FieldError("quantity", "MUST_BE_POSITIVE", "La cantidad debe ser mayor que cero.")
        }
        if (accountId.isNullOrBlank()) errors += required("accountId")
        if (errors.isNotEmpty()) reject(*errors.toTypedArray())
        return MarketOrder(
            symbol = requireNotNull(symbol),
            side = requireNotNull(side),
            quantity = requireNotNull(quantity),
            accountId = requireNotNull(accountId)
        )
    }

    private fun required(field: String) = FieldError(field, "REQUIRED", "El campo es obligatorio.")

    private fun reject(vararg errors: FieldError): Nothing =
        throw ProblemException(ProblemCode.VALIDATION_ERROR, "La orden tiene datos inválidos.", errors.toList())

    private fun reject(code: ProblemCode, detail: String): Nothing = throw ProblemException(code, detail)

    private companion object {
        const val OPERATION = "orders"
        const val CATEGORY = "INVESTMENT"
        const val CREATED = 201
    }
}
