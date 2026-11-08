package com.hacybeyker.cartera.feature.order.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveQuotesUseCase
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.feature.order.domain.model.OrderDraft
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.domain.model.OrderFormData
import com.hacybeyker.cartera.feature.order.domain.model.OrderValidation
import com.hacybeyker.cartera.feature.order.domain.usecase.LoadOrderFormUseCase
import com.hacybeyker.cartera.feature.order.domain.usecase.ValidateOrderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Formulario de una orden. Las cuentas y la tenencia salen de las cachés; el precio, del canal en vivo, y con él el
 * total estimado. Como en el Detalle, salir o pasar a segundo plano suelta la suscripción.
 */
@HiltViewModel(assistedFactory = OrderFormViewModel.Factory::class)
class OrderFormViewModel @AssistedInject constructor(
    @Assisted symbol: String,
    @Assisted side: OrderSide,
    private val loadOrderForm: LoadOrderFormUseCase,
    private val validateOrder: ValidateOrderUseCase,
    private val observeQuotes: ObserveQuotesUseCase,
    private val observeStreamStatus: ObserveMarketStreamStatusUseCase
) : ViewModel() {

    private val state = MutableStateFlow(OrderFormUiState(symbol = symbol, side = side))

    val uiState: StateFlow<OrderFormUiState> = channelFlow {
        launch { observeQuotes(setOf(symbol)).collect(::onQuote) }
        launch {
            observeStreamStatus().collect { status ->
                state.update { it.copy(isReconnecting = status == MarketStreamStatus.RECONNECTING) }
            }
        }
        state.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), state.value)

    init {
        load()
    }

    fun onAction(action: OrderFormAction) {
        when (action) {
            is OrderFormAction.QuantityChanged ->
                state.update { it.copy(quantity = action.value, fieldErrors = it.fieldErrors - OrderField.QUANTITY) }
            OrderFormAction.AccountClicked -> state.update { it.copy(isAccountSheetVisible = true) }
            is OrderFormAction.AccountSelected -> state.update {
                it.copy(
                    accountId = action.accountId,
                    isAccountChosen = true,
                    isAccountSheetVisible = false,
                    // Otra cuenta cambia el saldo con que se valida la cantidad.
                    fieldErrors = it.fieldErrors - OrderField.ACCOUNT - OrderField.QUANTITY
                )
            }
            OrderFormAction.AccountSheetDismissed -> state.update { it.copy(isAccountSheetVisible = false) }
            OrderFormAction.ContinueClicked -> validate()
            OrderFormAction.OrderOpened -> state.update { it.copy(order = null) }
            OrderFormAction.BackClicked -> state.update {
                if (it.hasData) it.copy(isExitConfirmationVisible = true) else it.copy(isExited = true)
            }
            OrderFormAction.ExitConfirmed ->
                state.update { it.copy(isExitConfirmationVisible = false, isExited = true) }
            OrderFormAction.ExitCancelled -> state.update { it.copy(isExitConfirmationVisible = false) }
            OrderFormAction.RetryClicked -> load()
        }
    }

    private fun onQuote(quote: Quote) {
        state.update { it.copy(name = quote.name, price = Money(quote.last, quote.currency)) }
    }

    // El total se valida con el precio en vivo de este momento, que viaja en la orden como precio de referencia.
    private fun validate() {
        val form = state.value
        val price = form.price ?: return
        val draft = OrderDraft(
            symbol = form.symbol,
            side = form.side,
            quantity = form.quantity,
            accountId = form.accountId,
            price = price
        )
        state.update {
            when (val validation = validateOrder(draft, OrderFormData(form.accounts, form.heldQuantity))) {
                is OrderValidation.Valid -> it.copy(fieldErrors = emptyMap(), order = validation.order)
                is OrderValidation.Invalid -> it.copy(fieldErrors = validation.errors)
            }
        }
    }

    private fun load() {
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val current = state.value
            val outcome = loadOrderForm(current.symbol, current.side)
            state.update { form ->
                when (outcome) {
                    is Outcome.Success -> form.copy(
                        isLoading = false,
                        accounts = outcome.value.accounts,
                        heldQuantity = outcome.value.heldQuantity,
                        // Con una sola cuenta posible, ya queda elegida.
                        accountId = form.accountId ?: outcome.value.accounts.singleOrNull()?.id,
                        contentVersion = form.contentVersion + 1
                    )
                    is Outcome.Failure -> form.copy(isLoading = false, error = outcome.error)
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(symbol: String, side: OrderSide): OrderFormViewModel
    }
}
