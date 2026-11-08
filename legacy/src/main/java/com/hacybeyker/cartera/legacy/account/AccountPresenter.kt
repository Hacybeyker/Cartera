package com.hacybeyker.cartera.legacy.account

import com.hacybeyker.cartera.legacy.account.domain.Account
import com.hacybeyker.cartera.legacy.account.domain.LoadPayableAccountsUseCase
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.RemoteResult
import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import com.hacybeyker.cartera.legacy.core.domain.toResult
import com.hacybeyker.cartera.legacy.core.mvp.BasePresenter
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.debts.domain.label
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import javax.inject.Inject

class AccountPresenter @Inject constructor(
    private val loadAccounts: LoadPayableAccountsUseCase,
    private val flow: PaymentFlow,
    private val schedulers: SchedulerProvider
) : BasePresenter<AccountView, AccountViewState>(
    AccountViewState(
        phase = LoadPhase.LOADING,
        billerName = checkNotNull(flow.biller).name,
        summary = flow.debts.map { SummaryLine(it.period.label(), it.amount.format()) },
        totalLabel = flow.total.format()
    )
) {

    private var accounts: List<Account> = emptyList()

    override fun onStart() = load()

    fun onAccountSelected(id: String) {
        if (id == state.selectedId) return
        val account = accounts.firstOrNull { it.id == id } ?: return
        update {
            it.copy(
                selectedId = account.id,
                insufficientFunds = account.availableBalance.amount < flow.total.amount
            )
        }
    }

    fun onContinue() {
        val account = accounts.firstOrNull { it.id == state.selectedId } ?: return
        flow.chooseAccount(account)
        emit { it.showPin() }
    }

    fun onRetry() = load()

    fun onCancel() {
        update { it.copy(error = null) }
        emit { it.goBack() }
    }

    private fun load() {
        update { it.copy(phase = LoadPhase.LOADING, error = null) }
        disposables.add(
            loadAccounts()
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.main())
                .toResult()
                .subscribe(::onResult)
        )
    }

    private fun onResult(result: RemoteResult<List<Account>>) {
        when (result) {
            is RemoteResult.Success -> onLoaded(result.value)
            is RemoteResult.Failure -> onFailure(result.error)
        }
    }

    private fun onLoaded(loaded: List<Account>) {
        accounts = loaded
        update {
            it.copy(
                phase = if (loaded.isEmpty()) LoadPhase.EMPTY else LoadPhase.CONTENT,
                options = loaded.map { account ->
                    AccountOption(account.id, account.alias, account.numberMasked, account.availableBalance.format())
                },
                selectedId = null,
                insufficientFunds = false
            )
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
