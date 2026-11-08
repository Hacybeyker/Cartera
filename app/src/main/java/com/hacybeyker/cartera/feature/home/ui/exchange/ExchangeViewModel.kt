package com.hacybeyker.cartera.feature.home.ui.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.GetAccountsRevisionUseCase
import com.hacybeyker.cartera.core.domain.account.GetAccountsUseCase
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.ObserveFxQuotesUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeValidation
import com.hacybeyker.cartera.feature.home.domain.model.PreparedExchange
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadExchangeUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.StartExchangeUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.ValidateExchangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cambio: se crea, y carga, la primera vez que se abre el tab; vive en la entrada de Home, como Mercado. */
@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val loadExchange: LoadExchangeUseCase,
    private val getAccounts: GetAccountsUseCase,
    private val getAccountsRevision: GetAccountsRevisionUseCase,
    private val validateExchange: ValidateExchangeUseCase,
    private val startExchange: StartExchangeUseCase,
    private val observeFxQuotes: ObserveFxQuotesUseCase,
    private val observeStreamStatus: ObserveMarketStreamStatusUseCase
) : ViewModel() {

    private val state = MutableStateFlow(ExchangeUiState())

    private var shownRevision = getAccountsRevision()

    /**
     * Las cotizaciones en vivo llegan solo mientras alguien recolecta este estado, y el tab lo hace solo mientras se ve
     * y en STARTED: al cambiar de tab, abrir la confirmación o pasar a segundo plano se suelta la suscripción.
     */
    val uiState: StateFlow<ExchangeUiState> = channelFlow {
        launch { observeFxQuotes().collect { quote -> state.update { it.withQuote(quote) } } }
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

    fun onAction(action: ExchangeAction) {
        when (action) {
            is ExchangeAction.AmountChanged ->
                state.update { it.copy(amount = action.value, fieldErrors = it.fieldErrors - ExchangeField.AMOUNT) }
            ExchangeAction.DirectionToggled -> state.update { it.reversed() }
            is ExchangeAction.AccountClicked -> state.update { it.copy(accountSheetFor = action.role) }
            is ExchangeAction.AccountSelected -> state.update { it.withAccount(action.accountId) }
            ExchangeAction.HowWeCalculateClicked -> state.update { it.copy(isHowWeCalculateVisible = true) }
            ExchangeAction.SheetDismissed ->
                state.update { it.copy(accountSheetFor = null, isHowWeCalculateVisible = false) }
            ExchangeAction.ContinueClicked -> continueExchange()
            ExchangeAction.OrderOpened -> state.update { it.copy(order = null) }
            ExchangeAction.ErrorDismissed -> state.update { it.copy(dialogError = null) }
            ExchangeAction.RetryClicked -> load()
            ExchangeAction.Shown -> onShown()
        }
    }

    private fun onShown() {
        val current = state.value
        val changed = getAccountsRevision() != shownRevision
        if (changed && !current.isLoading && current.error == null) refreshAccounts()
    }

    // Se valida primero con la cotización en vivo: con errores no hace falta pedir nada al servidor.
    private fun continueExchange() {
        val current = state.value
        val quote = current.quote
        if (quote == null || current.isPreparing) return
        val errors = validateExchange.errors(current.draft, current.accounts, quote)
        state.update { it.copy(fieldErrors = errors, dialogError = null, isPreparing = errors.isEmpty()) }
        if (errors.isEmpty()) prepare(current)
    }

    private fun prepare(current: ExchangeUiState) {
        viewModelScope.launch {
            val outcome = startExchange(current.draft, current.accounts)
            state.update { form ->
                when (outcome) {
                    is Outcome.Success -> form.withPrepared(outcome.value)
                    // El modal global de sesión expirada ya se ocupa; otro modal encima solo estorbaría.
                    is Outcome.Failure -> form.copy(
                        isPreparing = false,
                        dialogError = outcome.error.takeIf { it != AppError.SessionExpired }
                    )
                }
            }
        }
    }

    private fun ExchangeUiState.withPrepared(prepared: PreparedExchange): ExchangeUiState {
        val updated = copy(isPreparing = false).withQuote(prepared.quote)
        return when (val validation = prepared.validation) {
            is ExchangeValidation.Valid -> updated.copy(order = validation.order)
            is ExchangeValidation.Invalid -> updated.copy(fieldErrors = validation.errors)
        }
    }

    private fun load() {
        shownRevision = getAccountsRevision()
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val outcome = loadExchange()
            state.update { form ->
                when (outcome) {
                    is Outcome.Success -> form.copy(
                        isLoading = false,
                        accounts = outcome.value.accounts,
                        contentVersion = form.contentVersion + 1
                    ).withQuote(outcome.value.quote).withDefaultAccounts()
                    is Outcome.Failure -> form.copy(isLoading = false, error = outcome.error)
                }
            }
        }
    }

    // Lo que ya se eligió y escribió se conserva; solo cambian los saldos con que se valida.
    private fun refreshAccounts() {
        shownRevision = getAccountsRevision()
        viewModelScope.launch {
            when (val outcome = getAccounts()) {
                is Outcome.Success -> state.update {
                    it.copy(accounts = outcome.value, contentVersion = it.contentVersion + 1)
                }
                is Outcome.Failure -> state.update { it.copy(error = outcome.error) }
            }
        }
    }
}
