package com.hacybeyker.cartera.core.domain.exchange

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.testing.TestFxQuote
import java.math.BigDecimal
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExchangeDomainTest {

    @Test
    fun `the bank buys dollars at the buy rate and sells them at the sell rate`() {
        assertEquals(BigDecimal("3.742"), ExchangeDirection.USD_TO_PEN.rate(TestFxQuote))
        assertEquals(BigDecimal("3.768"), ExchangeDirection.PEN_TO_USD.rate(TestFxQuote))
        assertEquals(ExchangeDirection.PEN_TO_USD, ExchangeDirection.USD_TO_PEN.reversed)
        assertEquals(ExchangeDirection.USD_TO_PEN, ExchangeDirection.PEN_TO_USD.reversed)
    }

    @Test
    fun `dollars to soles multiply and soles to dollars divide, half up to the cent like the server`() {
        val buy = ExchangeDirection.USD_TO_PEN
        val sell = ExchangeDirection.PEN_TO_USD
        assertEquals(BigDecimal("374.20"), buy.credit(BigDecimal("100"), BigDecimal("3.742")))
        assertEquals(BigDecimal("100.00"), sell.credit(BigDecimal("376.80"), BigDecimal("3.768")))
        // 10 / 3.768 = 2.6539… y 0.01 × 3.742 = 0.03742.
        assertEquals(BigDecimal("2.65"), sell.credit(BigDecimal("10.00"), BigDecimal("3.768")))
        assertEquals(BigDecimal("0.04"), buy.credit(BigDecimal("0.01"), BigDecimal("3.742")))
        // 0.125 × 1 = 0.125 se redondea hacia arriba, no al par.
        assertEquals(BigDecimal("0.13"), buy.credit(BigDecimal("0.125"), BigDecimal.ONE))
        assertEquals(BigDecimal("0.00"), sell.credit(BigDecimal("0.01"), BigDecimal("3.768")))
    }

    @Test
    fun `an order with another quote recomputes the credit and takes the new key`() {
        val order = ExchangeOrder(
            direction = ExchangeDirection.USD_TO_PEN,
            quoteId = "QTE-000001",
            rate = BigDecimal("3.742"),
            expiresAt = TestFxQuote.expiresAt,
            source = ExchangeAccount.from(account("ACC-003", "USD")),
            destination = ExchangeAccount.from(account("ACC-002", "PEN")),
            amount = Money(BigDecimal("100.00"), "USD"),
            idempotencyKey = "key-1"
        )
        assertEquals(Money(BigDecimal("374.20"), "PEN"), order.credit)
        assertEquals("Cuenta ACC-002 ****0000", order.destination.label)

        val fresh = TestFxQuote.copy(
            quoteId = "QTE-000009",
            buy = BigDecimal("3.745"),
            expiresAt = OffsetDateTime.parse("2026-11-07T10:01:00-05:00")
        )
        val renewed = order.withQuote(fresh, "key-2")

        assertEquals("QTE-000009", renewed.quoteId)
        assertEquals(BigDecimal("3.745"), renewed.rate)
        assertEquals(fresh.expiresAt, renewed.expiresAt)
        assertEquals("key-2", renewed.idempotencyKey)
        assertEquals(Money(BigDecimal("374.50"), "PEN"), renewed.credit)
    }

    @Test
    fun `amounts follow the contract format`() {
        assertEquals(BigDecimal("100"), parseExchangeAmount(" 100 "))
        assertEquals(BigDecimal("100.5"), parseExchangeAmount("100.5"))
        assertEquals(BigDecimal("0.01"), parseExchangeAmount("0.01"))
        assertNull(parseExchangeAmount("1.234"))
        assertNull(parseExchangeAmount("-5"))
        assertNull(parseExchangeAmount("1,000"))
        assertNull(parseExchangeAmount("1e3"))
        assertNull(parseExchangeAmount(""))
        assertNull(parseExchangeAmount("1234567890"))
    }

    @Test
    fun `server field errors map to the fields of the exchange`() {
        val errors = exchangeFieldErrors(
            mapOf(
                "amount.value" to "TOO_SMALL",
                "sourceAccountId" to "NOT_OWNED",
                "destinationAccountId" to "REQUIRED",
                "quoteId" to "NOT_FOUND",
                "otro" to "X"
            )
        )

        assertEquals(
            mapOf(
                ExchangeField.AMOUNT to ExchangeFieldError.TOO_SMALL,
                ExchangeField.SOURCE to ExchangeFieldError.NOT_OWNED,
                ExchangeField.DESTINATION to ExchangeFieldError.REQUIRED,
                ExchangeField.QUOTE to ExchangeFieldError.NOT_FOUND
            ),
            errors
        )
        assertEquals(
            mapOf(ExchangeField.AMOUNT to ExchangeFieldError.INVALID_FORMAT),
            exchangeFieldErrors(mapOf("amount.currency" to "ALGO_NUEVO"))
        )
    }

    private fun account(id: String, currency: String) = Account(
        id = id,
        alias = "Cuenta $id",
        type = "SAVINGS",
        currency = currency,
        numberMasked = "****0000",
        availableBalance = Money(BigDecimal.TEN, currency),
        accountingBalance = Money(BigDecimal.TEN, currency)
    )
}
