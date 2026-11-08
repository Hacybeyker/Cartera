package com.hacybeyker.cartera.feature.order.domain.usecase

import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.idempotency.IdempotencyKeyFactory
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderDraft
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.domain.model.OrderFieldError
import com.hacybeyker.cartera.feature.order.domain.model.OrderFormData
import com.hacybeyker.cartera.feature.order.domain.model.OrderValidation
import com.hacybeyker.cartera.feature.order.domain.model.estimatedTotal
import javax.inject.Inject

/**
 * Valida en el cliente lo mismo que el contrato: cantidad entera mayor que cero, una cuenta en dólares propia, y que
 * alcance el saldo (compra, al precio en vivo) o la tenencia (venta). El precio con que ejecuta el servidor puede ser
 * otro, así que un 422 de fondos sigue siendo posible. Si todo está bien, la orden recibe una `Idempotency-Key` nueva:
 * cada Continuar es una operación distinta.
 */
class ValidateOrderUseCase @Inject constructor(private val keys: IdempotencyKeyFactory) {

    operator fun invoke(draft: OrderDraft, form: OrderFormData): OrderValidation {
        val account = form.accounts.firstOrNull { it.id == draft.accountId }
        val quantityText = draft.quantity.trim()
        val quantity = quantityText.takeIf { QUANTITY_FORMAT.matches(it) }?.toInt()
        val errors = buildMap {
            accountError(draft.accountId, account)?.let { put(OrderField.ACCOUNT, it) }
            quantityError(quantityText, quantity, draft, account, form.heldQuantity)?.let {
                put(OrderField.QUANTITY, it)
            }
        }
        if (errors.isNotEmpty() || account == null || quantity == null) return OrderValidation.Invalid(errors)
        return OrderValidation.Valid(
            MarketOrder(
                symbol = draft.symbol,
                side = draft.side,
                quantity = quantity,
                accountId = account.id,
                accountAlias = account.alias,
                accountNumberMasked = account.numberMasked,
                referencePrice = draft.price,
                idempotencyKey = keys.newKey()
            )
        )
    }

    private fun accountError(accountId: String?, account: Account?): OrderFieldError? = when {
        accountId == null -> OrderFieldError.REQUIRED
        account == null -> OrderFieldError.NOT_OWNED
        else -> null
    }

    private fun quantityError(
        text: String,
        quantity: Int?,
        draft: OrderDraft,
        account: Account?,
        held: Int
    ): OrderFieldError? = when {
        text.isEmpty() -> OrderFieldError.REQUIRED
        quantity == null -> OrderFieldError.INVALID_FORMAT
        quantity <= 0 -> OrderFieldError.MUST_BE_POSITIVE
        draft.side == OrderSide.SELL && quantity > held -> OrderFieldError.EXCEEDS_HOLDINGS
        draft.side == OrderSide.BUY &&
            account != null &&
            estimatedTotal(draft.price, quantity).amount > account.availableBalance.amount ->
            OrderFieldError.EXCEEDS_BALANCE
        else -> null
    }

    private companion object {
        // Hasta 9 dígitos: entra en un Int, como el `quantity` del contrato.
        val QUANTITY_FORMAT = Regex("""^\d{1,9}$""")
    }
}
