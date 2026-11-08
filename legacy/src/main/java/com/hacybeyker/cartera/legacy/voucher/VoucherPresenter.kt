package com.hacybeyker.cartera.legacy.voucher

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.RemoteResult
import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import com.hacybeyker.cartera.legacy.core.domain.toResult
import com.hacybeyker.cartera.legacy.core.mvp.BasePresenter
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.integration.LegacyContentMarker
import com.hacybeyker.cartera.legacy.integration.LegacyMarks
import com.hacybeyker.cartera.legacy.payment.domain.PaymentRepository
import javax.inject.Inject

class VoucherPresenter @Inject constructor(
    private val repository: PaymentRepository,
    private val flow: PaymentFlow,
    private val marker: LegacyContentMarker,
    private val schedulers: SchedulerProvider
) : BasePresenter<VoucherView, VoucherViewState>(VoucherViewState()) {

    private var marked = false

    override fun onStart() = load()

    /** La marca de contenido visible se avisa cuando el WebView terminó de cargar la constancia, una sola vez. */
    fun onPageFinished() {
        if (marked || state.phase != LoadPhase.CONTENT) return
        marked = true
        marker.onContentVisible(LegacyMarks.VOUCHER)
    }

    fun onRetry() = load()

    /** Sin constancia el pago sigue hecho: la pantalla queda con su mensaje y el botón Finalizar. */
    fun onCancel() {
        update { it.copy(error = null) }
    }

    fun onFinish() {
        emit { it.close() }
    }

    private fun load() {
        update { it.copy(phase = LoadPhase.LOADING, error = null) }
        disposables.add(
            repository.voucher(checkNotNull(flow.payment).paymentId)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .toResult()
                .subscribe(::onResult)
        )
    }

    private fun onResult(result: RemoteResult<String>) {
        when (result) {
            is RemoteResult.Success -> update { it.copy(phase = LoadPhase.CONTENT, html = result.value) }
            is RemoteResult.Failure -> onFailure(result.error)
        }
    }

    private fun onFailure(error: LegacyError) {
        if (error == LegacyError.SessionExpired) {
            emit { it.close() }
        } else {
            update { it.copy(phase = LoadPhase.FAILED, error = error) }
        }
    }
}
