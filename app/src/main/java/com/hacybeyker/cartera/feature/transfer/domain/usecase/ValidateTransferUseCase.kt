package com.hacybeyker.cartera.feature.transfer.domain.usecase

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferDraft
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferFieldError
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferValidation
import com.hacybeyker.cartera.feature.transfer.domain.repository.IdempotencyKeyFactory
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Valida en el cliente lo mismo que el contrato: origen propio, destino con formato, monto mayor que cero y no mayor
 * que el saldo disponible, descripción corta. La moneda del destino solo la conoce el servidor (`CURRENCY_MISMATCH`).
 * Si todo está bien, la orden recibe una `Idempotency-Key` nueva: cada Continuar es una operación distinta.
 */
class ValidateTransferUseCase @Inject constructor(private val keys: IdempotencyKeyFactory) {

    operator fun invoke(draft: TransferDraft, accounts: List<Account>): TransferValidation {
        val source = accounts.firstOrNull { it.id == draft.sourceAccountId }
        val destination = draft.destinationAccountNumber.trim()
        val amountText = draft.amount.trim()
        val amount = amountText.takeIf { AMOUNT_FORMAT.matches(it) }?.toBigDecimal()
        val description = draft.description.trim()
        val errors = buildMap {
            sourceError(draft.sourceAccountId, source)?.let { put(TransferField.SOURCE, it) }
            destinationError(destination)?.let { put(TransferField.DESTINATION, it) }
            amountError(amountText, amount, source)?.let { put(TransferField.AMOUNT, it) }
            if (description.length > MAX_DESCRIPTION) put(TransferField.DESCRIPTION, TransferFieldError.TOO_LONG)
        }
        if (errors.isNotEmpty() || source == null || amount == null) return TransferValidation.Invalid(errors)
        return TransferValidation.Valid(
            TransferOrder(
                sourceAccountId = source.id,
                sourceAlias = source.alias,
                sourceNumberMasked = source.numberMasked,
                destinationAccountNumber = destination,
                amount = Money(amount.setScale(2), source.currency),
                description = description.ifEmpty { null },
                idempotencyKey = keys.newKey()
            )
        )
    }

    private fun sourceError(sourceId: String?, source: Account?): TransferFieldError? = when {
        sourceId == null -> TransferFieldError.REQUIRED
        source == null -> TransferFieldError.NOT_OWNED
        else -> null
    }

    private fun destinationError(destination: String): TransferFieldError? = when {
        destination.isEmpty() -> TransferFieldError.REQUIRED
        !DESTINATION_FORMAT.matches(destination) -> TransferFieldError.INVALID_FORMAT
        else -> null
    }

    private fun amountError(text: String, amount: BigDecimal?, source: Account?): TransferFieldError? = when {
        text.isEmpty() -> TransferFieldError.REQUIRED
        amount == null -> TransferFieldError.INVALID_FORMAT
        amount.signum() <= 0 -> TransferFieldError.MUST_BE_POSITIVE
        source != null && amount > source.availableBalance.amount -> TransferFieldError.EXCEEDS_BALANCE
        else -> null
    }

    companion object {
        const val MAX_DESCRIPTION = 140
        private val AMOUNT_FORMAT = Regex("""^\d{1,9}(\.\d{1,2})?$""")
        private val DESTINATION_FORMAT = Regex("""^\d{3}-\d{7}-\d-\d{2}$""")
    }
}
