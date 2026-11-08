package com.hacybeyker.cartera.legacy.core.mvp

import io.reactivex.rxjava3.disposables.Disposable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BasePresenterTest {

    private class CounterView : MvpView<Int> {
        val rendered = mutableListOf<Int>()
        val events = mutableListOf<String>()

        override fun render(state: Int) {
            rendered += state
        }
    }

    private class CounterPresenter : BasePresenter<CounterView, Int>(0) {
        val order = mutableListOf<String>()
        val subscription: Disposable = Disposable.empty()

        init {
            disposables.add(subscription)
        }

        override fun onStart() {
            order += "start"
            update { 10 }
        }

        override fun onAttached() {
            order += "attached"
        }

        override fun onRendered(state: Int) {
            order += "rendered $state"
        }

        fun increment() = update { it + 1 }

        fun announce(text: String) = emit { it.events += text }

        override fun onDestroy() {
            order += "destroyed"
        }
    }

    @Test
    fun `the first view gets the state after start and attach hooks, once`() {
        val presenter = CounterPresenter()
        val view = CounterView()

        presenter.attach(view)

        assertEquals(listOf(10), view.rendered)
        assertEquals(listOf("start", "attached", "rendered 10"), presenter.order)
    }

    @Test
    fun `a new view gets the last state and start does not run again`() {
        val presenter = CounterPresenter()
        presenter.attach(CounterView())
        presenter.increment()
        presenter.detach()
        val second = CounterView()

        presenter.attach(second)

        assertEquals(listOf(11), second.rendered)
        assertEquals(1, presenter.order.count { it == "start" })
    }

    @Test
    fun `changes while no view is attached are kept and not delivered`() {
        val presenter = CounterPresenter()
        val view = CounterView()
        presenter.attach(view)
        presenter.detach()

        presenter.increment()

        assertEquals(listOf(10), view.rendered)
    }

    @Test
    fun `events wait for the next view and are delivered in order after the state`() {
        val presenter = CounterPresenter()
        presenter.attach(CounterView())
        presenter.detach()
        presenter.announce("a")
        presenter.announce("b")
        val next = CounterView()

        presenter.attach(next)

        assertEquals(listOf("a", "b"), next.events)
    }

    @Test
    fun `events with a view attached are delivered right away`() {
        val presenter = CounterPresenter()
        val view = CounterView()
        presenter.attach(view)

        presenter.announce("now")

        assertEquals(listOf("now"), view.events)
    }

    @Test
    fun `destroying detaches the view and disposes the subscriptions`() {
        val presenter = CounterPresenter()
        val view = CounterView()
        presenter.attach(view)

        presenter.destroy()
        presenter.increment()

        assertEquals(listOf(10), view.rendered)
        assertTrue(presenter.subscription.isDisposed)
    }

    @Test
    fun `destroying lets the presenter drop what must not outlive it, after the subscriptions`() {
        val presenter = CounterPresenter()
        presenter.attach(CounterView())

        presenter.destroy()

        assertEquals("destroyed", presenter.order.last())
        assertTrue(presenter.subscription.isDisposed)
    }
}
