package com.hacybeyker.cartera.feature.exchange.ui.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.core.domain.exchange.exchangeFieldErrors
import com.hacybeyker.cartera.core.domain.idempotency.IdempotencyKeyFactory
import com.hacybeyker.cartera.core.domain.market.GetFxRatesUseCase
import com.hacybeyker.cartera.feature.exchange.domain.usecase.ConfirmExchangeUseCase
import com.hacybeyker.cartera.feature.exchange.ui.isExchangeRejection
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import kotlinx.coroutines.delay
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

/**
 * Confirmación del cambio, con la cotización elegida en Continuar. No sigue al canal: la tasa es la de la cotización,
 * con su cuenta regresiva. Si vence (por el reloj o porque el servidor responde `QUOTE_EXPIRED`), ofrece la nueva.
 */
@HiltViewModel(assistedFactory = FxConfirmViewModel.Factory::class)
class FxConfirmViewModel @AssistedInject constructor(
    @Assisted order: ExchangeOrder,
    private val confirmExchange: ConfirmExchangeUseCase,
    private val getFxRates: GetFxRatesUseCase,
    private val keys: IdempotencyKeyFactory,
    private val clock: Clock
) : ViewModel() {

    private val state = MutableStateFlow(
        FxConfirmUiState(order = order, remainingSeconds = secondsUntil(order.expiresAt))
    )

    // La cotización que ya se intentó renovar sola: si falla (por ejemplo, por la sesión), no se insiste en un bucle.
    private var renewedFor: OffsetDateTime? = null

    /** La cuenta regresiva y la renovación corren solo mientras la pantalla recolecta el estado (STARTED). */
    val uiState: StateFlow<FxConfirmUiState> = channelFlow {
        launch { countDown() }
        launch { renewWhenExpired() }
        state.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), state.value)

    fun onAction(action: FxConfirmAction) {
        when (action) {
            FxConfirmAction.ConfirmClicked -> submit()
            FxConfirmAction.RetryClicked -> when (state.value.failure?.step) {
                FxConfirmStep.CONFIRM -> submit()
                FxConfirmStep.RENEW_QUOTE -> renewQuote()
                null -> Unit
            }
            FxConfirmAction.ErrorDismissed -> state.update {
                // Tras un envío sin respuesta no se sabe si el cambio se hizo; tras no poder renovar, no se envió nada.
                val sent = it.failure?.step == FxConfirmStep.CONFIRM
                it.copy(failure = null, exit = if (sent) FxConfirmExit.Failed else FxConfirmExit.BackToForm)
            }
            FxConfirmAction.RejectionAcknowledged ->
                state.update { it.copy(rejection = null, exit = FxConfirmExit.BackToForm) }
            FxConfirmAction.NewQuoteAccepted -> acceptNewQuote()
            FxConfirmAction.NewQuoteDeclined ->
                state.update { it.copy(newQuote = null, exit = FxConfirmExit.BackToForm) }
            FxConfirmAction.CancelClicked -> state.update { it.copy(isCancelConfirmationVisible = true) }
            FxConfirmAction.CancelConfirmed -> state.update {
                it.copy(isCancelConfirmationVisible = false, exit = FxConfirmExit.Cancelled)
            }
            FxConfirmAction.CancelDismissed -> state.update { it.copy(isCancelConfirmationVisible = false) }
        }
    }

    // Una vez por segundo, alineado al vencimiento; termina al llegar a 0 y vuelve a empezar con otra cotización.
    private suspend fun countDown() {
        state.map { it.order.expiresAt }.distinctUntilChanged().collectLatest { expiresAt ->
            do {
                val millis = millisUntil(expiresAt)
                val seconds = secondsOf(millis)
                state.update { it.copy(remainingSeconds = seconds) }
                if (millis > 0) delay(millis - (seconds - 1) * MILLIS_PER_SECOND)
            } while (millis > 0)
        }
    }

    // Con la cotización vencida y nada en curso, se pide una nueva. Si vence con un modal abierto, espera a que cierre.
    private suspend fun renewWhenExpired() {
        state.collect { current ->
            if (current.isQuoteExpired && current.isIdle && renewedFor != current.order.expiresAt) {
                renewedFor = current.order.expiresAt
                renewQuote()
            }
        }
    }

    private fun renewQuote() {
        state.update { it.copy(isRenewingQuote = true, failure = null) }
        viewModelScope.launch {
            val outcome = getFxRates()
            state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.copy(isRenewingQuote = false, newQuote = outcome.value)
                    // El modal global de sesión expirada ya se ocupa; otro modal encima solo estorbaría.
                    is Outcome.Failure -> current.copy(
                        isRenewingQuote = false,
                        failure = outcome.error.takeIf { it != AppError.SessionExpired }
                            ?.let { FxConfirmFailure(it, FxConfirmStep.RENEW_QUOTE) }
                    )
                }
            }
        }
    }

    /**
     * La cotización nueva cambia el cuerpo del envío, así que viaja con otra `Idempotency-Key`. Es seguro: solo se
     * llega aquí tras un `QUOTE_EXPIRED` o sin haber enviado nada, así que la clave anterior no hizo ningún cambio.
     */
    private fun acceptNewQuote() {
        state.update { current ->
            val quote = current.newQuote ?: return@update current
            current.copy(
                order = current.order.withQuote(quote, keys.newKey()),
                newQuote = null,
                fieldErrors = emptyMap(),
                remainingSeconds = secondsUntil(quote.expiresAt)
            )
        }
    }

    // El cambio lleva su Idempotency-Key: Reintentar lo reenvía igual y el servidor no lo hace dos veces.
    private fun submit() {
        if (state.value.isBusy) return
        state.update { it.copy(isSubmitting = true, failure = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val order = state.value.order
            when (val outcome = confirmExchange(order)) {
                is Outcome.Success ->
                    state.update { it.copy(isSubmitting = false, exit = FxConfirmExit.Done(outcome.value)) }
                is Outcome.Failure -> onFailure(order, outcome.error)
            }
        }
    }

    private fun onFailure(order: ExchangeOrder, error: AppError) {
        if (error is AppError.QuoteExpired) {
            renewedFor = order.expiresAt
            state.update { it.copy(isSubmitting = false, newQuote = error.quote) }
            // Sin una cotización nueva legible en la respuesta, se pide otra.
            if (error.quote == null) renewQuote()
            return
        }
        state.update { current ->
            when {
                error is AppError.Validation ->
                    current.copy(isSubmitting = false, fieldErrors = exchangeFieldErrors(error.fieldCodes))
                error.isExchangeRejection -> current.copy(isSubmitting = false, rejection = error)
                error == AppError.SessionExpired -> current.copy(isSubmitting = false)
                else -> current.copy(isSubmitting = false, failure = FxConfirmFailure(error, FxConfirmStep.CONFIRM))
            }
        }
    }

    private fun millisUntil(instant: OffsetDateTime): Long =
        Duration.between(clock.instant(), instant.toInstant()).toMillis()

    private fun secondsUntil(instant: OffsetDateTime): Long = secondsOf(millisUntil(instant))

    private fun secondsOf(millis: Long): Long =
        if (millis <= 0) 0 else (millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND

    @AssistedFactory
    interface Factory {
        fun create(order: ExchangeOrder): FxConfirmViewModel
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
