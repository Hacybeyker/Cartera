package com.hacybeyker.cartera.legacy.debts

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.core.domain.RemoteResult
import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import com.hacybeyker.cartera.legacy.core.domain.toResult
import com.hacybeyker.cartera.legacy.core.mvp.BasePresenter
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.debts.domain.CustomerDebts
import com.hacybeyker.cartera.legacy.debts.domain.DebtRepository
import com.hacybeyker.cartera.legacy.debts.domain.DebtSelection
import com.hacybeyker.cartera.legacy.debts.domain.label
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.integration.LegacyContentMarker
import com.hacybeyker.cartera.legacy.integration.LegacyMarks
import javax.inject.Inject

class DebtsPresenter @Inject constructor(
    private val repository: DebtRepository,
    private val flow: PaymentFlow,
    private val marker: LegacyContentMarker,
    private val schedulers: SchedulerProvider
) : BasePresenter<DebtsView, DebtsViewState>(DebtsViewState()) {

    private var customerName = ""
    private var selection = DebtSelection(emptyList())
    private var marked = false

    override fun onStart() = load()

    override fun onRendered(state: DebtsViewState) {
        if (!marked && (state.phase == LoadPhase.CONTENT || state.phase == LoadPhase.EMPTY)) {
            marked = true
            marker.onContentVisible(LegacyMarks.DEBTS)
        }
    }

    fun onDebtToggled(id: String) {
        selection = selection.toggle(id)
        update { content() }
    }

    fun onContinue() {
        if (selection.selected.isEmpty()) return
        flow.chooseDebts(customerName, selection.selected)
        emit { it.showAccount() }
    }

    fun onRetry() = load()

    fun onCancel() {
        update { it.copy(error = null) }
        emit { it.goBack() }
    }

    private fun load() {
        update { it.copy(phase = LoadPhase.LOADING, error = null) }
        val biller = checkNotNull(flow.biller)
        val code = checkNotNull(flow.customerCode)
        disposables.add(
            repository.pendingDebts(biller.id, code)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .toResult()
                .subscribe(::onResult)
        )
    }

    private fun onResult(result: RemoteResult<CustomerDebts>) {
        when (result) {
            is RemoteResult.Success -> onLoaded(result.value)
            is RemoteResult.Failure -> onFailure(result.error)
        }
    }

    private fun onLoaded(loaded: CustomerDebts) {
        customerName = loaded.customerName
        selection = DebtSelection(loaded.debts)
        update { content() }
    }

    private fun onFailure(error: LegacyError) {
        when {
            error == LegacyError.SessionExpired -> emit { it.close() }
            error.isUnknownCustomer() -> {
                flow.rejectCustomerCode()
                emit { it.returnToCustomerCode() }
            }
            else -> update { it.copy(phase = LoadPhase.FAILED, error = error) }
        }
    }

    private fun content(): DebtsViewState {
        val items = selection.debts.map { debt ->
            DebtItem(
                id = debt.id,
                periodLabel = debt.period.label(),
                dueDateLabel = debt.dueDate.label(),
                amountLabel = debt.amount.format(),
                checked = selection.isSelected(debt.id)
            )
        }
        return DebtsViewState(
            phase = if (items.isEmpty()) LoadPhase.EMPTY else LoadPhase.CONTENT,
            customerName = customerName,
            items = items,
            totalLabel = selection.total.format(),
            canContinue = selection.selected.isNotEmpty()
        )
    }
}

private fun LegacyError.isUnknownCustomer(): Boolean =
    this is LegacyError.Rejected && code == ProblemCode.CUSTOMER_NOT_FOUND
