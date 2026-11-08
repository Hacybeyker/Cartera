package com.hacybeyker.cartera.feature.home.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.FakeMarketRepository
import com.hacybeyker.cartera.core.testing.SequentialKeys
import com.hacybeyker.cartera.core.testing.TestFxQuote
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeDraft
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeOverview
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeValidation
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadExchangeUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.StartExchangeUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.ValidateExchangeUseCase
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeUseCasesTest {
    private val accounts = FakeAccountRepository(previewAccounts)
    private val market = FakeMarketRepository()
    private val validate = ValidateExchangeUseCase(SequentialKeys())

    /** El cambio de referencia: US$ 100 de la Cuenta Dólares a la Cuenta Gastos. */
    private val reference = ExchangeDraft(ExchangeDirection.USD_TO_PEN, "100", "ACC-003", "ACC-002")

    @Test
    fun `load asks for the accounts from the cache and for a quote`() = runTest {
        val outcome = LoadExchangeUseCase(accounts, market)()

        assertEquals(Outcome.Success(ExchangeOverview(previewAccounts, TestFxQuote)), outcome)
        assertEquals(1, accounts.accountsCalls)
        assertEquals(0, accounts.fetchCalls)
        assertEquals(1, market.fxCalls)
    }

    @Test
    fun `load fails if either call fails`() = runTest {
        accounts.error = AppError.Network
        assertEquals(Outcome.Failure(AppError.Network), LoadExchangeUseCase(accounts, market)())

        accounts.error = null
        market.error = AppError.Server
        assertEquals(Outcome.Failure(AppError.Server), LoadExchangeUseCase(accounts, market)())
    }

    @Test
    fun `the reference exchange is valid and credits at the buy rate with a new key`() {
        val order = (validate(reference, previewAccounts, TestFxQuote) as ExchangeValidation.Valid).order

        assertEquals("QTE-000001", order.quoteId)
        assertEquals(BigDecimal("3.742"), order.rate)
        assertEquals(TestFxQuote.expiresAt, order.expiresAt)
        assertEquals("ACC-003", order.source.id)
        assertEquals("Cuenta Dólares ****1177", order.source.label)
        assertEquals("ACC-002", order.destination.id)
        assertEquals(Money(BigDecimal("100.00"), "USD"), order.amount)
        assertEquals(Money(BigDecimal("374.20"), "PEN"), order.credit)
        assertEquals("key-1", order.idempotencyKey)
        // Cada Continuar es otra operación.
        val again = validate(reference, previewAccounts, TestFxQuote) as ExchangeValidation.Valid
        assertEquals("key-2", again.order.idempotencyKey)
    }

    @Test
    fun `soles to dollars credits at the sell rate`() {
        val draft = ExchangeDraft(ExchangeDirection.PEN_TO_USD, "376.80", "ACC-002", "ACC-003")

        val order = (validate(draft, previewAccounts, TestFxQuote) as ExchangeValidation.Valid).order

        assertEquals(BigDecimal("3.768"), order.rate)
        assertEquals(Money(BigDecimal("376.80"), "PEN"), order.amount)
        assertEquals(Money(BigDecimal("100.00"), "USD"), order.credit)
    }

    @Test
    fun `an empty draft asks for every field`() {
        val draft = ExchangeDraft(ExchangeDirection.USD_TO_PEN, " ", null, null)

        assertEquals(
            ExchangeValidation.Invalid(
                mapOf(
                    ExchangeField.SOURCE to ExchangeFieldError.REQUIRED,
                    ExchangeField.DESTINATION to ExchangeFieldError.REQUIRED,
                    ExchangeField.AMOUNT to ExchangeFieldError.REQUIRED
                )
            ),
            validate(draft, previewAccounts, TestFxQuote)
        )
    }

    @Test
    fun `the amount needs the contract format, more than zero and enough balance`() {
        assertEquals(ExchangeFieldError.INVALID_FORMAT, amountError("100.555"))
        assertEquals(ExchangeFieldError.INVALID_FORMAT, amountError("-10"))
        assertEquals(ExchangeFieldError.MUST_BE_POSITIVE, amountError("0.00"))
        assertEquals(ExchangeFieldError.EXCEEDS_BALANCE, amountError("1050.21"))
        assertEquals(null, amountError("1050.20"))
        // S/ 0.01 a 3.768 no llega a un centavo de dólar.
        assertEquals(
            ExchangeFieldError.TOO_SMALL,
            validate.errors(
                ExchangeDraft(ExchangeDirection.PEN_TO_USD, "0.01", "ACC-002", "ACC-003"),
                previewAccounts,
                TestFxQuote
            )[ExchangeField.AMOUNT]
        )
    }

    @Test
    fun `accounts must be own and of the currencies of the direction`() {
        val errors = validate.errors(
            ExchangeDraft(ExchangeDirection.USD_TO_PEN, "10", "ACC-001", "ACC-404"),
            previewAccounts,
            TestFxQuote
        )

        assertEquals(ExchangeFieldError.WRONG_CURRENCY, errors[ExchangeField.SOURCE])
        assertEquals(ExchangeFieldError.NOT_OWNED, errors[ExchangeField.DESTINATION])
        // Sin un origen válido no se puede comparar el saldo: el monto solo se valida por su forma.
        assertEquals(null, errors[ExchangeField.AMOUNT])
    }

    @Test
    fun `start asks for a fresh quote and validates with it`() = runTest {
        val fresh = TestFxQuote.copy(quoteId = "QTE-000002", buy = BigDecimal("3.745"))
        market.fxQuote = fresh

        val prepared = (StartExchangeUseCase(market, validate)(reference, previewAccounts) as Outcome.Success).value

        assertEquals(fresh, prepared.quote)
        val order = (prepared.validation as ExchangeValidation.Valid).order
        assertEquals("QTE-000002", order.quoteId)
        assertEquals(Money(BigDecimal("374.50"), "PEN"), order.credit)
        assertEquals(1, market.fxCalls)
    }

    @Test
    fun `start reports a failed quote and a draft that is no longer valid with the fresh one`() = runTest {
        market.fxQuotes += Outcome.Failure(AppError.Network)
        val start = StartExchangeUseCase(market, validate)
        assertEquals(Outcome.Failure(AppError.Network), start(reference, previewAccounts))

        // Con una venta mucho más cara, S/ 0.02 ya no alcanza para un centavo.
        market.fxQuote = TestFxQuote.copy(sell = BigDecimal("4.100"))
        val draft = ExchangeDraft(ExchangeDirection.PEN_TO_USD, "0.02", "ACC-002", "ACC-003")
        val prepared = (start(draft, previewAccounts) as Outcome.Success).value
        assertTrue(prepared.validation is ExchangeValidation.Invalid)
    }

    private fun amountError(amount: String) =
        validate.errors(reference.copy(amount = amount), previewAccounts, TestFxQuote)[ExchangeField.AMOUNT]
}
