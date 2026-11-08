package com.hacybeyker.cartera.feature.home.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.domain.flags.IsFeatureEnabledUseCase
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveQuotesUseCase
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioRevisionUseCase
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioUseCase
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadMarketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mercado: se crea, y carga, la primera vez que se abre el tab; vive en la entrada de Home. */
@HiltViewModel
class MarketViewModel @Inject constructor(
    private val loadMarket: LoadMarketUseCase,
    private val getPortfolio: GetPortfolioUseCase,
    private val getPortfolioRevision: GetPortfolioRevisionUseCase,
    private val observeQuotes: ObserveQuotesUseCase,
    private val observeStreamStatus: ObserveMarketStreamStatusUseCase,
    private val isFeatureEnabled: IsFeatureEnabledUseCase
) : ViewModel() {

    private val state = MutableStateFlow(MarketUiState(fullRefresh = isFullRefresh()))

    // Lo que se necesita para revalorizar la cabecera; la UI solo ve sus totales.
    private var portfolio: Portfolio? = null
    private val lastQuotes = mutableMapOf<String, Quote>()

    private var shownRevision = getPortfolioRevision()

    /**
     * Los precios en vivo llegan solo mientras alguien recolecta este estado, y la pantalla lo hace solo en STARTED:
     * al cambiar de tab, abrir otra pantalla o pasar a segundo plano se suelta la suscripción enseguida, y el canal
     * se cierra si nadie más lo usa.
     */
    val uiState: StateFlow<MarketUiState> = channelFlow {
        launch { followQuotes() }
        launch { followStatus() }
        state.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), state.value)

    init {
        load()
    }

    fun onAction(action: MarketAction) {
        when (action) {
            MarketAction.RetryClicked -> load()
            MarketAction.Shown -> {
                state.update { it.copy(fullRefresh = isFullRefresh()) }
                if (!state.value.isLoading && getPortfolioRevision() != shownRevision) refresh()
            }
        }
    }

    // Se decide cada vez que el tab vuelve a verse: este ViewModel vive en la entrada de Home, así un cambio del
    // interruptor rige sin volver a iniciar sesión.
    private fun isFullRefresh(): Boolean = isFeatureEnabled(FeatureFlag.QUOTES_FULL_REFRESH)

    private suspend fun followQuotes() {
        state.map { it.symbols }.distinctUntilChanged().collectLatest { symbols ->
            observeQuotes(symbols).collect(::onQuote)
        }
    }

    private suspend fun followStatus() {
        observeStreamStatus().collect { status ->
            state.update { it.copy(isReconnecting = status == MarketStreamStatus.RECONNECTING) }
        }
    }

    private fun onQuote(quote: Quote) {
        lastQuotes[quote.symbol] = quote
        val current = portfolio
        val revalued = current?.takeIf { it.holds(quote.symbol) }?.withQuote(quote)
        if (revalued != null) portfolio = revalued
        state.update { it.withQuote(quote, revalued?.let(PortfolioHeader::from)) }
    }

    private fun load() {
        shownRevision = getPortfolioRevision()
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = loadMarket()) {
                is Outcome.Success -> {
                    val market = outcome.value
                    portfolio = market.portfolio
                    lastQuotes.clear()
                    market.stocks.associateByTo(lastQuotes) { it.symbol }
                    state.update { current ->
                        current.copy(
                            isLoading = false,
                            stocks = market.stocks.map(StockItem::from),
                            portfolio = PortfolioHeader.from(market.portfolio),
                            contentVersion = current.contentVersion + 1
                        )
                    }
                }
                is Outcome.Failure -> state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    // Las acciones siguen en vivo: solo se vuelve a pedir el portafolio, valorizado con los últimos precios.
    private fun refresh() {
        shownRevision = getPortfolioRevision()
        viewModelScope.launch {
            when (val outcome = getPortfolio()) {
                is Outcome.Success -> {
                    val revalued = outcome.value.withQuotes(lastQuotes.values)
                    portfolio = revalued
                    state.update {
                        it.copy(portfolio = PortfolioHeader.from(revalued), contentVersion = it.contentVersion + 1)
                    }
                }
                is Outcome.Failure -> state.update { it.copy(error = outcome.error) }
            }
        }
    }
}
