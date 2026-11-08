package com.hacybeyker.cartera.feature.order.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.FakePortfolioChanges
import com.hacybeyker.cartera.core.testing.FakePortfolioRepository
import com.hacybeyker.cartera.core.testing.SequentialKeys
import com.hacybeyker.cartera.feature.order.DollarAccount
import com.hacybeyker.cartera.feature.order.FakeOrderRepository
import com.hacybeyker.cartera.feature.order.OrderTestAccounts
import com.hacybeyker.cartera.feature.order.ReferenceBuy
import com.hacybeyker.cartera.feature.order.ReferenceBuyReceipt
import com.hacybeyker.cartera.feature.order.domain.model.OrderDraft
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.domain.model.OrderFieldError
import com.hacybeyker.cartera.feature.order.domain.model.OrderFormData
import com.hacybeyker.cartera.feature.order.domain.model.OrderValidation
import com.hacybeyker.cartera.feature.order.domain.model.orderFieldErrors
import com.hacybeyker.cartera.feature.order.domain.usecase.ConfirmOrderUseCase
import com.hacybeyker.cartera.feature.order.domain.usecase.LoadOrderFormUseCase
import com.hacybeyker.cartera.feature.order.domain.usecase.ValidateOrderUseCase
import com.hacybeyker.cartera.feature.order.usd
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderUseCasesTest {
    private val validate = ValidateOrderUseCase(SequentialKeys())
    private val form = OrderFormData(accounts = listOf(DollarAccount), heldQuantity = 2)

    private fun draft(quantity: String, side: OrderSide = OrderSide.BUY, accountId: String? = "ACC-003") =
        OrderDraft(symbol = "ANDM", side = side, quantity = quantity, accountId = accountId, price = usd("42.10"))

    private fun errorsOf(draft: OrderDraft, data: OrderFormData = form) =
        (validate(draft, data) as OrderValidation.Invalid).errors

    @Test
    fun `the reference buy is valid, totals US$ 84,20 and gets a new key each time`() {
        val first = (validate(draft("2"), form) as OrderValidation.Valid).order
        val second = (validate(draft(" 2 "), form) as OrderValidation.Valid).order

        assertEquals(ReferenceBuy, first)
        assertEquals(usd("84.20"), first.estimatedTotal)
        assertEquals("key-2", second.idempotencyKey)
    }

    @Test
    fun `quantity must be a whole number greater than zero`() {
        assertEquals(mapOf(OrderField.QUANTITY to OrderFieldError.REQUIRED), errorsOf(draft("")))
        assertEquals(mapOf(OrderField.QUANTITY to OrderFieldError.INVALID_FORMAT), errorsOf(draft("1.5")))
        assertEquals(mapOf(OrderField.QUANTITY to OrderFieldError.INVALID_FORMAT), errorsOf(draft("-1")))
        assertEquals(mapOf(OrderField.QUANTITY to OrderFieldError.INVALID_FORMAT), errorsOf(draft("1234567890")))
        assertEquals(mapOf(OrderField.QUANTITY to OrderFieldError.MUST_BE_POSITIVE), errorsOf(draft("0")))
    }

    @Test
    fun `the account is required and must be one of the offered ones`() {
        assertEquals(mapOf(OrderField.ACCOUNT to OrderFieldError.REQUIRED), errorsOf(draft("1", accountId = null)))
        assertEquals(
            mapOf(OrderField.ACCOUNT to OrderFieldError.NOT_OWNED),
            errorsOf(draft("1", accountId = "ACC-001"))
        )
    }

    @Test
    fun `a buy cannot exceed the available balance at the live price`() {
        // 24 × 42.10 = 1,010.40 alcanza; 25 × 42.10 = 1,052.50 supera los 1,050.20.
        assertEquals(OrderValidation.Valid::class, validate(draft("24"), form)::class)
        assertEquals(mapOf(OrderField.QUANTITY to OrderFieldError.EXCEEDS_BALANCE), errorsOf(draft("25")))
    }

    @Test
    fun `a sell cannot exceed the holding, and selling all of it is valid`() {
        val sellAll = validate(draft("2", OrderSide.SELL), form) as OrderValidation.Valid

        assertEquals(2, sellAll.order.quantity)
        assertEquals(
            mapOf(OrderField.QUANTITY to OrderFieldError.EXCEEDS_HOLDINGS),
            errorsOf(draft("3", OrderSide.SELL))
        )
        assertEquals(
            mapOf(OrderField.QUANTITY to OrderFieldError.EXCEEDS_HOLDINGS),
            errorsOf(draft("1", OrderSide.SELL), form.copy(heldQuantity = 0))
        )
    }

    @Test
    fun `the form offers only dollar accounts and asks for the portfolio only to sell`() = runTest {
        val accounts = FakeAccountRepository(OrderTestAccounts)
        val portfolio = FakePortfolioRepository()
        val load = LoadOrderFormUseCase(accounts, portfolio)

        val buy = (load("QORI", OrderSide.BUY) as Outcome.Success).value
        assertEquals(OrderFormData(listOf(DollarAccount), heldQuantity = 0), buy)
        assertEquals(0, portfolio.calls)

        val sell = (load("QORI", OrderSide.SELL) as Outcome.Success).value
        assertEquals(2, sell.heldQuantity)
        assertEquals(0, (load("LIMA", OrderSide.SELL) as Outcome.Success).value.heldQuantity)
        // Siempre desde la caché: nunca pide las cuentas por fuera de ella.
        assertEquals(3, accounts.accountsCalls)
        assertEquals(0, accounts.fetchCalls)
        assertEquals(2, portfolio.calls)
    }

    @Test
    fun `the form fails if the accounts or the portfolio fail`() = runTest {
        val accounts = FakeAccountRepository(OrderTestAccounts)
        val portfolio = FakePortfolioRepository(error = AppError.Server)
        val load = LoadOrderFormUseCase(accounts, portfolio)

        assertEquals(Outcome.Failure(AppError.Server), load("QORI", OrderSide.SELL))
        accounts.error = AppError.Network
        assertEquals(Outcome.Failure(AppError.Network), load("QORI", OrderSide.BUY))
    }

    @Test
    fun `a confirmed order, or one that may have run, invalidates accounts and portfolio`() = runTest {
        listOf(null, AppError.Network, AppError.Server).forEach { failure ->
            val orders = FakeOrderRepository().apply { failure?.let { failures += it } }
            val accountChanges = FakeAccountChanges()
            val portfolioChanges = FakePortfolioChanges()

            val outcome = ConfirmOrderUseCase(orders, accountChanges, portfolioChanges)(ReferenceBuy)

            assertEquals(failure?.let { Outcome.Failure(it) } ?: Outcome.Success(ReferenceBuyReceipt), outcome)
            assertEquals(1, accountChanges.revision)
            assertEquals(1, portfolioChanges.revision)
        }
    }

    @Test
    fun `a rejected order invalidates nothing`() = runTest {
        val rejections = listOf(
            AppError.InsufficientFunds,
            AppError.InsufficientHoldings,
            AppError.Validation(mapOf("quantity" to "MUST_BE_POSITIVE")),
            AppError.SessionExpired
        )
        rejections.forEach { error ->
            val orders = FakeOrderRepository().apply { failures += error }
            val accountChanges = FakeAccountChanges()
            val portfolioChanges = FakePortfolioChanges()

            ConfirmOrderUseCase(orders, accountChanges, portfolioChanges)(ReferenceBuy)

            assertEquals(0, accountChanges.revision)
            assertEquals(0, portfolioChanges.revision)
        }
    }

    @Test
    fun `validation errors map the contract fields and codes`() {
        val errors = orderFieldErrors(
            mapOf(
                "symbol" to "UNKNOWN_SYMBOL",
                "side" to "INVALID_VALUE",
                "quantity" to "MUST_BE_POSITIVE",
                "accountId" to "UNSUPPORTED_CURRENCY",
                "Idempotency-Key" to "REQUIRED"
            )
        )

        assertEquals(
            mapOf(
                OrderField.SYMBOL to OrderFieldError.UNKNOWN_SYMBOL,
                OrderField.SIDE to OrderFieldError.INVALID_FORMAT,
                OrderField.QUANTITY to OrderFieldError.MUST_BE_POSITIVE,
                OrderField.ACCOUNT to OrderFieldError.UNSUPPORTED_CURRENCY
            ),
            errors
        )
    }
}
