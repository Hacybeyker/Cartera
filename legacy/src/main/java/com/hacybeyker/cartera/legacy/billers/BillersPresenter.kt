package com.hacybeyker.cartera.legacy.billers

import com.hacybeyker.cartera.legacy.billers.domain.Biller
import com.hacybeyker.cartera.legacy.billers.domain.BillerRepository
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.RemoteResult
import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import com.hacybeyker.cartera.legacy.core.domain.toResult
import com.hacybeyker.cartera.legacy.core.mvp.BasePresenter
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.integration.LegacyContentMarker
import com.hacybeyker.cartera.legacy.integration.LegacyFeatureFlags
import com.hacybeyker.cartera.legacy.integration.LegacyMarks
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val SEARCH_DEBOUNCE_MILLIS = 300L

class BillersPresenter @Inject constructor(
    private val repository: BillerRepository,
    private val flow: PaymentFlow,
    private val marker: LegacyContentMarker,
    private val flags: LegacyFeatureFlags,
    private val schedulers: SchedulerProvider
) : BasePresenter<BillersView, BillersViewState>(BillersViewState()) {

    private val typed = PublishSubject.create<String>()
    private val typedFullRebind = PublishSubject.create<String>()
    private val reloads = PublishSubject.create<String>()
    private var billers: List<Biller> = emptyList()
    private var requestedQuery: String? = null
    private var marked = false

    override fun onStart() {
        disposables.add(
            Observable.merge(
                typed.debounce(SEARCH_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS, schedulers.computation())
                    .observeOn(schedulers.main())
                    .filter { it != requestedQuery },
                typedFullRebind.filter { it != requestedQuery },
                reloads
            )
                .doOnNext {
                    requestedQuery = it
                    update { state -> state.copy(phase = LoadPhase.LOADING, error = null) }
                }
                .switchMapSingle(::search)
                .subscribe(::onResult)
        )
        reloads.onNext("")
    }

    override fun onRendered(state: BillersViewState) {
        if (!marked && state.phase == LoadPhase.CONTENT) {
            marked = true
            marker.onContentVisible(LegacyMarks.BILLERS)
        }
    }

    fun onQueryChanged(text: String) {
        if (text == state.query) return
        val fullRebind = flags.billersFullRebind()
        update { it.copy(query = text, fullRebind = fullRebind) }
        if (fullRebind) typedFullRebind.onNext(text.trim()) else typed.onNext(text.trim())
    }

    fun onBillerSelected(id: String) {
        val biller = billers.firstOrNull { it.id == id } ?: return
        flow.chooseBiller(biller)
        emit { it.showCustomerCode() }
    }

    fun onRetry() {
        reloads.onNext(state.query.trim())
    }

    fun onCancel() {
        update { it.copy(error = null) }
        emit { it.goBack() }
    }

    private fun search(query: String): Single<RemoteResult<List<Biller>>> = repository.search(query)
        .subscribeOn(schedulers.io())
        .observeOn(schedulers.main())
        .toResult()

    private fun onResult(result: RemoteResult<List<Biller>>) = when (result) {
        is RemoteResult.Success -> {
            billers = result.value
            val rows = result.value.toRows()
            val fullRebind = flags.billersFullRebind()
            update {
                it.copy(
                    phase = if (rows.isEmpty()) LoadPhase.EMPTY else LoadPhase.CONTENT,
                    rows = rows,
                    resultsFor = requestedQuery,
                    fullRebind = fullRebind
                )
            }
        }
        is RemoteResult.Failure -> onFailure(result.error)
    }

    private fun onFailure(error: LegacyError) {
        if (error == LegacyError.SessionExpired) {
            emit { it.close() }
        } else {
            update { it.copy(phase = LoadPhase.FAILED, error = error) }
        }
    }

    private fun List<Biller>.toRows(): List<BillerRow> = groupBy { it.category }.flatMap { (category, group) ->
        listOf<BillerRow>(BillerRow.Header(category)) + group.map { BillerRow.Item(it.id, it.name) }
    }
}
