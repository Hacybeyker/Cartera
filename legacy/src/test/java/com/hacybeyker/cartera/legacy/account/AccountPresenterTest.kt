package com.hacybeyker.cartera.legacy.account

import com.hacybeyker.cartera.legacy.account.domain.LoadPayableAccountsUseCase
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.testing.FakeAccountRepository
import com.hacybeyker.cartera.legacy.testing.RecordingAccountView
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.TestSchedulerProvider
import com.hacybeyker.cartera.legacy.testing.failure
import io.reactivex.rxjava3.core.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountPresenterTest {
    private val schedulers = TestSchedulerProvider()
    private val repository = FakeAccountRepository().apply {
        respond = { Single.just(listOf(Samples.sueldo, Samples.gastos, Samples.dolares)) }
    }
    private val flow = PaymentFlow().apply {
        chooseBiller(Samples.luz)
        chooseCustomerCode("100200")
        chooseDebts("Ana Torres", Samples.luzDebts.debts)
    }
    private val presenter = AccountPresenter(LoadPayableAccountsUseCase(repository), flow, schedulers)
    private val view = RecordingAccountView()

    private fun settle() = schedulers.scheduler.triggerActions()

    private fun loaded() {
        presenter.attach(view)
        settle()
    }

    @Test
    fun `the summary of the debts and the total show while the accounts load`() {
        presenter.attach(view)

        assertEquals(LoadPhase.LOADING, view.last.phase)
        assertEquals("Luz del Norte", view.last.billerName)
        assertEquals(
            listOf(SummaryLine("Octubre 2026", "S/ 85.40"), SummaryLine("Noviembre 2026", "S/ 92.10")),
            view.last.summary
        )
        assertEquals("S/ 177.50", view.last.totalLabel)
    }

    @Test
    fun `only the accounts in soles are offered and none is chosen`() {
        loaded()

        assertEquals(LoadPhase.CONTENT, view.last.phase)
        assertEquals(
            listOf(
                AccountOption("ACC-001", "Cuenta Sueldo", "****4821", "S/ 8,450.75"),
                AccountOption("ACC-002", "Cuenta Gastos", "****9034", "S/ 2,300.00")
            ),
            view.last.options
        )
        assertNull(view.last.selectedId)
        assertFalse(view.last.canContinue)
    }

    @Test
    fun `choosing an account enables continue`() {
        loaded()

        presenter.onAccountSelected("ACC-002")

        assertEquals("ACC-002", view.last.selectedId)
        assertTrue(view.last.canContinue)
        assertFalse(view.last.insufficientFunds)
    }

    @Test
    fun `a balance below the total warns but does not block`() {
        repository.respond = { Single.just(listOf(Samples.poca, Samples.gastos)) }
        loaded()

        presenter.onAccountSelected("ACC-004")

        assertTrue(view.last.insufficientFunds)
        assertTrue(view.last.canContinue)
        presenter.onAccountSelected("ACC-002")
        assertFalse(view.last.insufficientFunds)
    }

    @Test
    fun `a balance equal to the total does not warn`() {
        repository.respond = {
            Single.just(
                listOf(Samples.gastos.copy(availableBalance = com.hacybeyker.cartera.legacy.testing.pen("177.50")))
            )
        }
        loaded()

        presenter.onAccountSelected("ACC-002")

        assertFalse(view.last.insufficientFunds)
    }

    @Test
    fun `choosing the same account again or an unknown one changes nothing`() {
        loaded()
        presenter.onAccountSelected("ACC-002")
        val shown = view.states.size

        presenter.onAccountSelected("ACC-002")
        presenter.onAccountSelected("ACC-999")

        assertEquals(shown, view.states.size)
    }

    @Test
    fun `continuing keeps the account in the flow and opens the pin`() {
        loaded()
        presenter.onAccountSelected("ACC-002")

        presenter.onContinue()

        assertEquals(Samples.gastos, flow.account)
        assertEquals(listOf("showPin"), view.events)
    }

    @Test
    fun `continuing without an account does nothing`() {
        loaded()

        presenter.onContinue()

        assertNull(flow.account)
        assertEquals(emptyList<String>(), view.events)
    }

    @Test
    fun `no account in soles is an empty state`() {
        repository.respond = { Single.just(listOf(Samples.dolares)) }

        loaded()

        assertEquals(LoadPhase.EMPTY, view.last.phase)
        assertEquals(emptyList<AccountOption>(), view.last.options)
    }

    @Test
    fun `an error shows the dialog state and retry loads again dropping the choice`() {
        repository.respond = { Single.error(failure(LegacyError.Network)) }
        loaded()
        assertEquals(LoadPhase.FAILED, view.last.phase)
        assertEquals(LegacyError.Network, view.last.error)

        repository.respond = { Single.just(listOf(Samples.gastos)) }
        presenter.onRetry()
        settle()

        assertEquals(LoadPhase.CONTENT, view.last.phase)
        assertNull(view.last.error)
        assertEquals(2, repository.requests)
    }

    @Test
    fun `cancelling the error goes back`() {
        repository.respond = { Single.error(failure(LegacyError.Server)) }
        loaded()

        presenter.onCancel()

        assertEquals(listOf("goBack"), view.events)
    }

    @Test
    fun `an expired session closes the module`() {
        repository.respond = { Single.error(failure(LegacyError.SessionExpired)) }

        loaded()

        assertEquals(listOf("close"), view.events)
    }
}
