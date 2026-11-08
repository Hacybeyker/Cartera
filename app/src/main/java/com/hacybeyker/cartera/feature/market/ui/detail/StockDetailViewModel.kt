package com.hacybeyker.cartera.feature.market.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.GetPriceHistoryUseCase
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveQuotesUseCase
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.portfolio.Holding
import com.hacybeyker.cartera.feature.market.domain.model.StockOverview
import com.hacybeyker.cartera.feature.market.domain.usecase.LoadStockDetailUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Detalle de una acción. El precio, la tenencia y, en 1D, el último punto del gráfico siguen al canal en vivo; los
 * rangos largos se dibujan tal como se cargaron, con el último punto en el precio de ese momento.
 */
@HiltViewModel(assistedFactory = StockDetailViewModel.Factory::class)
class StockDetailViewModel @AssistedInject constructor(
    @Assisted private val symbol: String,
    private val loadStockDetail: LoadStockDetailUseCase,
    private val getPriceHistory: GetPriceHistoryUseCase,
    private val observeQuotes: ObserveQuotesUseCase,
    private val observeStreamStatus: ObserveMarketStreamStatusUseCase
) : ViewModel() {

    private val state = MutableStateFlow(StockDetailUiState())

    private var holding: Holding? = null
    private var lastQuote: Quote? = null
    private var historyJob: Job? = null

    /**
     * Se suscribe al símbolo desde que la pantalla empieza a recolectar, aun antes de terminar la carga: al venir de
     * Mercado el canal sigue abierto. Como en Mercado, salir o pasar a segundo plano suelta la suscripción.
     */
    val uiState: StateFlow<StockDetailUiState> = channelFlow {
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

    fun onAction(action: StockDetailAction) {
        when (action) {
            StockDetailAction.RetryClicked -> load()
            StockDetailAction.ChartRetryClicked -> loadHistory(state.value.range)
            is StockDetailAction.RangeSelected -> if (action.range != state.value.range) {
                state.update { it.copy(range = action.range) }
                loadHistory(action.range)
            }
        }
    }

    private fun onQuote(quote: Quote) {
        lastQuote = quote
        if (state.value.price == null) return
        holding = holding?.withQuote(quote)
        state.update {
            it.copy(
                price = LivePrice.from(quote),
                holding = holding?.let(HoldingSummary::from),
                chart = if (it.range == HistoryRange.DAY) it.chart.withLivePrice(quote.last) else it.chart
            )
        }
    }

    private fun load() {
        historyJob?.cancel()
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = loadStockDetail(symbol, state.value.range)) {
                is Outcome.Success -> show(outcome.value)
                is Outcome.Failure -> state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    // Un precio del canal que llegó durante la carga gana si es más nuevo que el de la respuesta.
    private fun show(overview: StockOverview) {
        val quote = lastQuote?.takeIf { it.ts.isAfter(overview.stock.quote.ts) } ?: overview.stock.quote
        lastQuote = quote
        holding = overview.holding?.withQuote(quote)
        state.update { current ->
            current.copy(
                isLoading = false,
                name = quote.name,
                price = LivePrice.from(quote),
                day = DayData.from(overview.stock),
                holding = holding?.let(HoldingSummary::from),
                chart = ChartUiState(
                    isLoading = false,
                    chart = PriceChart.from(overview.history.points.map { it.price }, quote.last)
                ),
                contentVersion = current.contentVersion + 1
            )
        }
    }

    private fun loadHistory(range: HistoryRange) {
        val live = lastQuote ?: return
        historyJob?.cancel()
        state.update { it.copy(chart = ChartUiState(isLoading = true)) }
        historyJob = viewModelScope.launch {
            when (val outcome = getPriceHistory(symbol, range)) {
                is Outcome.Success -> state.update { current ->
                    // El último punto empalma con el precio más reciente, que pudo cambiar mientras se cargaba.
                    val price = (lastQuote ?: live).last
                    current.copy(
                        chart = ChartUiState(
                            isLoading = false,
                            chart = PriceChart.from(outcome.value.points.map { it.price }, price)
                        ),
                        contentVersion = current.contentVersion + 1
                    )
                }
                is Outcome.Failure -> state.update {
                    it.copy(chart = ChartUiState(isLoading = false, error = outcome.error))
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(symbol: String): StockDetailViewModel
    }
}
