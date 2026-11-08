package com.hacybeyker.cartera.feature.transfer.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.core.testing.TestAccounts
import com.hacybeyker.cartera.core.testing.pen
import com.hacybeyker.cartera.feature.transfer.FakeTransferRepository
import com.hacybeyker.cartera.feature.transfer.ReferenceOrder
import com.hacybeyker.cartera.feature.transfer.ReferenceReceipt
import com.hacybeyker.cartera.feature.transfer.SequentialKeys
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferDraft
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferFieldError
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferValidation
import com.hacybeyker.cartera.feature.transfer.domain.model.transferFieldErrors
import com.hacybeyker.cartera.feature.transfer.domain.usecase.ConfirmTransferUseCase
import com.hacybeyker.cartera.feature.transfer.domain.usecase.ValidateTransferUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferUseCasesTest {
    private val validate = ValidateTransferUseCase(SequentialKeys())

    private fun draft(
        source: String? = "ACC-002",
        destination: String = "999-0000001-0-01",
        amount: String = "150",
        description: String = ""
    ) = TransferDraft(source, destination, amount, description)

    private fun errorsOf(draft: TransferDraft) = (validate(draft, TestAccounts) as TransferValidation.Invalid).errors

    @Test
    fun `the reference transfer is valid and gets a key`() {
        val order = (validate(draft(description = "  Almuerzo "), TestAccounts) as TransferValidation.Valid).order

        assertEquals("ACC-002", order.sourceAccountId)
        assertEquals("Cuenta Gastos", order.sourceAlias)
        assertEquals(pen("150.00"), order.amount)
        assertEquals("150.00", order.amount.amount.toPlainString())
        assertEquals("Almuerzo", order.description)
        assertEquals("key-1", order.idempotencyKey)
    }

    @Test
    fun `each valid continue is a new operation with a new key`() {
        val first = (validate(draft(), TestAccounts) as TransferValidation.Valid).order
        val second = (validate(draft(), TestAccounts) as TransferValidation.Valid).order

        assertEquals("key-1", first.idempotencyKey)
        assertEquals("key-2", second.idempotencyKey)
    }

    @Test
    fun `an empty form reports every required field`() {
        assertEquals(
            mapOf(
                TransferField.SOURCE to TransferFieldError.REQUIRED,
                TransferField.DESTINATION to TransferFieldError.REQUIRED,
                TransferField.AMOUNT to TransferFieldError.REQUIRED
            ),
            errorsOf(draft(source = null, destination = " ", amount = ""))
        )
    }

    @Test
    fun `source must be an own account`() {
        assertEquals(mapOf(TransferField.SOURCE to TransferFieldError.NOT_OWNED), errorsOf(draft(source = "ACC-999")))
    }

    @Test
    fun `destination needs the account number format`() {
        listOf("9990000001001", "999-000001-0-01", "abc-0000001-0-01").forEach {
            assertEquals(
                mapOf(TransferField.DESTINATION to TransferFieldError.INVALID_FORMAT),
                errorsOf(draft(destination = it))
            )
        }
    }

    @Test
    fun `amount must be a positive decimal with up to two decimals`() {
        listOf("1.234", "1,50", "abc", "-5").forEach {
            assertEquals(mapOf(TransferField.AMOUNT to TransferFieldError.INVALID_FORMAT), errorsOf(draft(amount = it)))
        }
        listOf("0", "0.00").forEach {
            assertEquals(
                mapOf(TransferField.AMOUNT to TransferFieldError.MUST_BE_POSITIVE),
                errorsOf(draft(amount = it))
            )
        }
    }

    @Test
    fun `amount can be the whole balance but not more`() {
        assertEquals(TransferValidation.Valid::class, validate(draft(amount = "2300.00"), TestAccounts)::class)
        assertEquals(
            mapOf(TransferField.AMOUNT to TransferFieldError.EXCEEDS_BALANCE),
            errorsOf(draft(amount = "2300.01"))
        )
    }

    @Test
    fun `description has at most 140 characters`() {
        assertEquals(
            TransferValidation.Valid::class,
            validate(draft(description = "a".repeat(140)), TestAccounts)::class
        )
        assertEquals(
            mapOf(TransferField.DESCRIPTION to TransferFieldError.TOO_LONG),
            errorsOf(draft(description = "a".repeat(141)))
        )
    }

    @Test
    fun `server field errors map to the form fields`() {
        assertEquals(
            mapOf(
                TransferField.SOURCE to TransferFieldError.NOT_OWNED,
                TransferField.AMOUNT to TransferFieldError.MUST_BE_POSITIVE,
                TransferField.DESCRIPTION to TransferFieldError.TOO_LONG,
                TransferField.DESTINATION to TransferFieldError.INVALID_FORMAT
            ),
            transferFieldErrors(
                mapOf(
                    "sourceAccountId" to "NOT_OWNED",
                    "amount.value" to "MUST_BE_POSITIVE",
                    "description" to "TOO_LONG",
                    "destinationAccountNumber" to "SOMETHING_NEW",
                    "Idempotency-Key" to "REQUIRED"
                )
            )
        )
    }

    @Test
    fun `a confirmed transfer changes the accounts`() = runTest {
        val transfers = FakeTransferRepository()
        val changes = FakeAccountChanges()

        assertEquals(Outcome.Success(ReferenceReceipt), ConfirmTransferUseCase(transfers, changes)(ReferenceOrder))
        assertEquals(1, changes.revision)
        assertEquals(listOf(ReferenceOrder), transfers.sent)
    }

    @Test
    fun `without an answer the transfer may have charged, so the accounts change too`() = runTest {
        val transfers = FakeTransferRepository().apply { failures += AppError.Network }
        val changes = FakeAccountChanges()

        ConfirmTransferUseCase(transfers, changes)(ReferenceOrder)

        assertEquals(1, changes.revision)
    }

    @Test
    fun `after a server error the transfer may have charged, so the accounts change too`() = runTest {
        val transfers = FakeTransferRepository().apply { failures += AppError.Server }
        val changes = FakeAccountChanges()

        ConfirmTransferUseCase(transfers, changes)(ReferenceOrder)

        assertEquals(1, changes.revision)
    }

    @Test
    fun `a rejected transfer does not change the accounts`() = runTest {
        val transfers = FakeTransferRepository()
        val changes = FakeAccountChanges()
        val confirm = ConfirmTransferUseCase(transfers, changes)

        listOf(AppError.InsufficientFunds, AppError.DestinationNotFound, AppError.CurrencyMismatch)
            .forEach { error ->
                transfers.failures += error
                assertEquals(Outcome.Failure(error), confirm(ReferenceOrder))
            }

        assertEquals(0, changes.revision)
    }
}
