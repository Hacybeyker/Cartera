package com.hacybeyker.cartera.feature.market.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveQuotesUseCase
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioRevisionUseCase
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioUseCase
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
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

/** Mi portafolio: las tenencias se revalorizan con cada precio del canal en vivo, fila por fila. */
@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val getPortfolio: GetPortfolioUseCase,
    private val getPortfolioRevision: GetPortfolioRevisionUseCase,
    private val observeQuotes: ObserveQuotesUseCase,
    private val observeStreamStatus: ObserveMarketStreamStatusUseCase
) : ViewModel() {

    private val state = MutableStateFlow(PortfolioUiState())

    private var portfolio: Portfolio? = null
    private val lastQuotes = mutableMapOf<String, Quote>()

    private var shownRevision = getPortfolioRevision()

    /** Como en Mercado, los precios llegan solo mientras la pantalla recolecta este estado (en STARTED). */
    val uiState: StateFlow<PortfolioUiState> = channelFlow {
        launch { followQuotes() }
        launch {
            observeStreamStatus().collect { status ->
                state.update { it.copy(isReconnecting = status == MarketStreamStatus.RECONNECTING) }
            }
        }
        state.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), state.value)

    init {
        fetch(showLoading = true)
    }

    fun onAction(action: PortfolioAction) {
        when (action) {
            PortfolioAction.RetryClicked -> fetch(showLoading = true)
            PortfolioAction.Shown -> if (!state.value.isLoading && getPortfolioRevision() != shownRevision) {
                fetch(showLoading = false)
            }
        }
    }

    // Sin tenencias no hay nada que seguir, y el canal no se abre.
    private suspend fun followQuotes() {
        state.map { it.symbols }.distinctUntilChanged().collectLatest { symbols ->
            if (symbols.isNotEmpty()) observeQuotes(symbols).collect(::onQuote)
        }
    }

    private fun onQuote(quote: Quote) {
        lastQuotes[quote.symbol] = quote
        val current = portfolio?.takeIf { it.holds(quote.symbol) } ?: return
        val revalued = current.withQuote(quote)
        portfolio = revalued
        state.update { it.revalued(revalued, quote.symbol) }
    }

    // Un refresco tras una orden mantiene la lista a la vista mientras llega; si falla, muestra el error.
    private fun fetch(showLoading: Boolean) {
        shownRevision = getPortfolioRevision()
        if (showLoading) state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = getPortfolio()) {
                is Outcome.Success -> {
                    val revalued = outcome.value.withQuotes(lastQuotes.values)
                    portfolio = revalued
                    state.update { it.loaded(revalued) }
                }
                is Outcome.Failure -> state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }
}
