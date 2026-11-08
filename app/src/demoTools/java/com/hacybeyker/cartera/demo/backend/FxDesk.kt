package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Cotización emitida: el servidor recuerda su tasa hasta [expiresAt]. */
data class FxQuote(
    val id: String,
    val userId: String,
    val rate: FxTick,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val ttlSeconds: Int
)

data class ExchangeOrder(
    val quoteId: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: BigDecimal,
    val currency: String
)

/** Cambio de divisas entre cuentas propias, con una cotización que vence. */
class FxDesk(private val state: BackendState, private val clock: Clock) {

    fun rates(userId: String, request: BackendRequest): BackendResponse =
        json(OK, quoteJson(issue(userId, DemoScenario.from(request).quoteTtlSeconds)))

    /** Emite una cotización con el tipo de cambio vigente. El canal en vivo publica la misma forma como `fx`. */
    fun issue(userId: String, ttlSeconds: Int): FxQuote {
        val issuedAt = clock.instant()
        val quote = FxQuote(
            id = "QTE-%06d".format(state.nextSequence(QUOTES)),
            userId = userId,
            rate = state.market.fx().tick,
            issuedAt = issuedAt,
            expiresAt = issuedAt.plusSeconds(ttlSeconds.toLong()),
            ttlSeconds = ttlSeconds
        )
        state.fxQuotes[quote.id] = quote
        return quote
    }

    fun quoteJson(quote: FxQuote): JsonObject = buildJsonObject {
        put("pair", PAIR)
        put("buy", quote.rate.buy.toPlainString())
        put("sell", quote.rate.sell.toPlainString())
        put("quoteId", quote.id)
        put("expiresAt", timestamp(quote.expiresAt))
        put("ts", timestamp(quote.issuedAt))
    }

    fun exchange(userId: String, idempotencyKey: String?, body: JsonObject): BackendResponse {
        if (idempotencyKey.isNullOrBlank()) {
            reject(FieldError(TransferDesk.IDEMPOTENCY_HEADER, "REQUIRED", "La cabecera es obligatoria."))
        }
        return state.idempotency.once("$OPERATION:$userId:$idempotencyKey") { execute(userId, parse(body)) }
    }

    private fun execute(userId: String, order: ExchangeOrder): BackendResponse {
        val source = state.account(userId, order.sourceAccountId)
            ?: reject(FieldError("sourceAccountId", "NOT_OWNED", "La cuenta de origen no es tuya."))
        val destination = state.account(userId, order.destinationAccountId)
            ?: reject(FieldError("destinationAccountId", "NOT_OWNED", "La cuenta de destino no es tuya."))
        val quote = state.fxQuotes[order.quoteId]?.takeIf { it.userId == userId }
            ?: reject(FieldError("quoteId", "NOT_FOUND", "La cotización no existe."))
        val sourceCurrency = source.string("currency")
        val bankBuysDollars = when (sourceCurrency to destination.string("currency")) {
            DOLLARS to SOLES -> true
            SOLES to DOLLARS -> false
            else -> reject(
                ProblemCode.CURRENCY_MISMATCH,
                "El cambio es solo entre una cuenta en soles y una en dólares."
            )
        }
        if (order.currency != sourceCurrency) {
            reject(ProblemCode.CURRENCY_MISMATCH, "El monto debe estar en $sourceCurrency, la moneda de origen.")
        }
        if (!clock.instant().isBefore(quote.expiresAt)) expired(userId, quote)
        if (order.amount > source.availableBalance()) {
            reject(ProblemCode.INSUFFICIENT_FUNDS, "El monto supera el saldo disponible.")
        }
        val rate = if (bankBuysDollars) quote.rate.buy else quote.rate.sell
        val credited = if (bankBuysDollars) {
            cents(order.amount * rate)
        } else {
            order.amount.divide(rate, 2, RoundingMode.HALF_UP)
        }
        if (credited.signum() <= 0) {
            reject(FieldError("amount.value", "TOO_SMALL", "El monto no alcanza para un centavo de la otra moneda."))
        }
        return executed(userId, order, Conversion(rate, credited, source, destination))
    }

    private fun expired(userId: String, quote: FxQuote): Nothing = throw ProblemException(
        code = ProblemCode.QUOTE_EXPIRED,
        detail = "La cotización venció. Revisa la nueva antes de confirmar.",
        extensions = mapOf("quote" to quoteJson(issue(userId, quote.ttlSeconds)))
    )

    private data class Conversion(
        val rate: BigDecimal,
        val credited: BigDecimal,
        val source: JsonObject,
        val destination: JsonObject
    ) {
        val sourceCurrency: String get() = source.string("currency")
        val destinationCurrency: String get() = destination.string("currency")
    }

    private fun executed(userId: String, order: ExchangeOrder, conversion: Conversion): BackendResponse {
        val sequence = state.nextSequence(OPERATION)
        val createdAt = timestamp(clock.instant())
        val operationNumber = "8%07d".format(sequence)
        val sourceBalance = conversion.source.availableBalance() - order.amount
        val destinationBalance = conversion.destination.availableBalance() + conversion.credited
        val description = if (conversion.sourceCurrency == DOLLARS) {
            "Cambio de dólares a soles"
        } else {
            "Cambio de soles a dólares"
        }
        val holder = state.user(userId)?.string("fullName").orEmpty()
        val legs = listOf(
            Leg(order.sourceAccountId, order.amount.negate(), conversion.sourceCurrency, sourceBalance),
            Leg(order.destinationAccountId, conversion.credited, conversion.destinationCurrency, destinationBalance)
        )
        val movementIds = legs.map { leg ->
            val movementId = "MOV-${leg.accountId.substringAfter("ACC-")}-X%04d".format(sequence)
            val counterparty = legs.first { it !== leg }.accountId
            val movement = leg.movement(
                Leg.Info(movementId, operationNumber, createdAt, description, conversion.rate),
                counterparty = holder to counterparty
            )
            state.post(leg.accountId, leg.newBalance, MovementRecord.from(movement))
            movementId
        }
        return json(
            status = CREATED,
            body = buildJsonObject {
                put("exchangeId", "FX-%06d".format(sequence))
                put("operationNumber", operationNumber)
                put("createdAt", createdAt)
                put("quoteId", order.quoteId)
                put("rate", conversion.rate.toPlainString())
                put("debited", money(order.amount, conversion.sourceCurrency))
                put("credited", money(conversion.credited, conversion.destinationCurrency))
                put("sourceAccountId", order.sourceAccountId)
                put("sourceAvailableBalance", money(sourceBalance, conversion.sourceCurrency))
                put("destinationAccountId", order.destinationAccountId)
                put("destinationAvailableBalance", money(destinationBalance, conversion.destinationCurrency))
                putJsonArray("movementIds") { movementIds.forEach { add(it) } }
            }
        )
    }

    private data class Leg(
        val accountId: String,
        val amount: BigDecimal,
        val currency: String,
        val newBalance: BigDecimal
    ) {
        data class Info(
            val movementId: String,
            val operationNumber: String,
            val createdAt: String,
            val description: String,
            val rate: BigDecimal
        )

        fun movement(info: Info, counterparty: Pair<String, String>): JsonObject = buildJsonObject {
            put("id", info.movementId)
            put("accountId", accountId)
            put("date", info.createdAt)
            put("description", info.description)
            put("category", CATEGORY)
            put("amount", money(amount, currency))
            put("type", if (amount.signum() < 0) "DEBIT" else "CREDIT")
            put("operationNumber", info.operationNumber)
            put("channel", "APP")
            put("status", "COMPLETED")
            putJsonObject("counterparty") {
                put("name", counterparty.first)
                put("reference", counterparty.second)
            }
            putJsonObject("breakdown") {
                putJsonArray("lines") {
                    addJsonObject {
                        put("concept", "Cambio")
                        put("amount", money(amount.abs(), currency))
                    }
                }
                put("fees", money(BigDecimal.ZERO, currency))
                put("taxes", money(BigDecimal.ZERO, currency))
                put("exchangeRate", info.rate.setScale(BREAKDOWN_RATE_SCALE).toPlainString())
                put("merchant", JsonNull)
                put("category", CATEGORY)
            }
        }
    }

    private fun parse(body: JsonObject): ExchangeOrder {
        val errors = mutableListOf<FieldError>()
        val quoteId = body.text("quoteId")
        val source = body.text("sourceAccountId")
        val destination = body.text("destinationAccountId")
        val amount = body["amount"] as? JsonObject
        val value = amount?.text("value")
        val currency = amount?.text("currency")
        listOf("quoteId" to quoteId, "sourceAccountId" to source, "destinationAccountId" to destination)
            .filter { (_, text) -> text.isNullOrBlank() }
            .forEach { (field, _) -> errors += required(field) }
        if (value == null || !AMOUNT_FORMAT.matches(value)) {
            errors += FieldError("amount.value", "INVALID_FORMAT", "Usa un decimal con hasta dos decimales.")
        } else if (BigDecimal(value).signum() <= 0) {
            errors += FieldError("amount.value", "MUST_BE_POSITIVE", "El monto debe ser mayor que cero.")
        }
        if (currency == null || !CURRENCY_FORMAT.matches(currency)) {
            errors += FieldError("amount.currency", "INVALID_FORMAT", "Usa un código ISO 4217.")
        }
        if (errors.isNotEmpty()) reject(*errors.toTypedArray())
        return ExchangeOrder(
            quoteId = requireNotNull(quoteId),
            sourceAccountId = requireNotNull(source),
            destinationAccountId = requireNotNull(destination),
            amount = BigDecimal(value).setScale(2),
            currency = requireNotNull(currency)
        )
    }

    private fun required(field: String) = FieldError(field, "REQUIRED", "El campo es obligatorio.")

    private fun reject(vararg errors: FieldError): Nothing =
        throw ProblemException(ProblemCode.VALIDATION_ERROR, "El cambio tiene datos inválidos.", errors.toList())

    private fun reject(code: ProblemCode, detail: String): Nothing = throw ProblemException(code, detail)

    companion object {
        const val PAIR = "USD/PEN"
        private const val DOLLARS = "USD"
        private const val SOLES = "PEN"
        private const val OPERATION = "exchanges"
        private const val QUOTES = "quotes"
        private const val CATEGORY = "TRANSFER"
        private const val OK = 200
        private const val CREATED = 201
        private const val BREAKDOWN_RATE_SCALE = 4
        private val AMOUNT_FORMAT = Regex("""\d{1,9}(\.\d{1,2})?""")
        private val CURRENCY_FORMAT = Regex("[A-Z]{3}")
    }
}
