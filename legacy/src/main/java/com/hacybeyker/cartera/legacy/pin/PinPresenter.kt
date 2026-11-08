package com.hacybeyker.cartera.legacy.pin

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.RemoteResult
import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import com.hacybeyker.cartera.legacy.core.domain.toResult
import com.hacybeyker.cartera.legacy.core.mvp.BasePresenter
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.integration.LegacyPinPadSeed
import com.hacybeyker.cartera.legacy.payment.domain.BillPayment
import com.hacybeyker.cartera.legacy.payment.domain.IdempotencyKeyFactory
import com.hacybeyker.cartera.legacy.payment.domain.PaymentOrder
import com.hacybeyker.cartera.legacy.payment.domain.PaymentRepository
import com.hacybeyker.cartera.legacy.pin.domain.PinFailure
import com.hacybeyker.cartera.legacy.pin.domain.PinKeyboard
import com.hacybeyker.cartera.legacy.pin.domain.PinNotice
import com.hacybeyker.cartera.legacy.pin.domain.toPinFailure
import javax.inject.Inject

class PinPresenter @Inject constructor(
    private val repository: PaymentRepository,
    private val flow: PaymentFlow,
    private val padSeed: LegacyPinPadSeed,
    private val idempotencyKeys: IdempotencyKeyFactory,
    private val schedulers: SchedulerProvider
) : BasePresenter<PinView, PinViewState>(PinViewState()) {

    private val keyboard = PinKeyboard()
    private var pending: PaymentOrder? = null

    private val acceptsInput: Boolean
        get() = !state.sending && state.error == null && state.notice == null

    override fun onStart() {
        keyboard.start(padSeed.current())
        update { it.copy(order = keyboard.order) }
    }

    override fun onDestroy() {
        keyboard.clear()
        pending = null
    }

    fun onDigit(digit: Int) {
        if (!acceptsInput || !keyboard.append(digit)) return
        update { it.copy(filled = keyboard.filled, invalidPin = false) }
        if (keyboard.isFull) pay(flow.paymentOrder(keyboard.takePin(), idempotencyKeys.create()))
    }

    fun onDelete() {
        if (!acceptsInput || !keyboard.deleteLast()) return
        update { it.copy(filled = keyboard.filled) }
    }

    /** Reintentar tras un error de red o del servidor: la misma orden con la misma clave de idempotencia. */
    fun onRetry() {
        pending?.let(::pay)
    }

    fun onCancel() {
        pending = null
        update { it.copy(error = null, sending = false, filled = 0) }
        emit { it.goBack() }
    }

    fun onNoticeAccepted() {
        val notice = state.notice ?: return
        update { it.copy(notice = null) }
        emit { if (notice == PinNotice.DEBT_ALREADY_PAID) it.reloadDebts() else it.goBack() }
    }

    private fun pay(order: PaymentOrder) {
        pending = order
        update { it.copy(sending = true, error = null) }
        disposables.add(
            repository.pay(order)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .toResult()
                .subscribe(::onResult)
        )
    }

    private fun onResult(result: RemoteResult<BillPayment>) {
        when (result) {
            is RemoteResult.Success -> {
                pending = null
                flow.completePayment(result.value)
                emit { it.showVoucher() }
            }
            is RemoteResult.Failure -> onFailure(result.error)
        }
    }

    private fun onFailure(error: LegacyError) {
        when (val failure = error.toPinFailure()) {
            PinFailure.SessionExpired -> {
                pending = null
                emit { it.close() }
            }
            PinFailure.WrongPin -> {
                pending = null
                keyboard.rejectPin()
                update { it.copy(order = keyboard.order, filled = 0, sending = false, invalidPin = true) }
            }
            is PinFailure.Rejected -> {
                pending = null
                update { it.copy(sending = false, filled = 0, notice = failure.notice) }
            }
            is PinFailure.Retryable -> {
                flow.markMayHaveCharged()
                update { it.copy(sending = false, error = failure.error) }
            }
        }
    }
}
