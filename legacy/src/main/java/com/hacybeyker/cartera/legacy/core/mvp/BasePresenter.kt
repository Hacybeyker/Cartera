package com.hacybeyker.cartera.legacy.core.mvp

import io.reactivex.rxjava3.disposables.CompositeDisposable

/**
 * Presenter sin conocimiento de Android. Conserva el último estado y se lo entrega a cada vista que se engancha (la
 * vista se destruye y se crea de nuevo al rotar o al volver a la pantalla; el Presenter, no). Los eventos de una sola
 * vez (navegar, cerrar) que ocurren sin vista esperan a la próxima.
 */
abstract class BasePresenter<V : MvpView<S>, S : Any>(initialState: S) {

    protected val disposables = CompositeDisposable()

    protected var state: S = initialState
        private set

    private var view: V? = null
    private var started = false
    private val pendingEvents = ArrayDeque<(V) -> Unit>()

    fun attach(view: V) {
        if (!started) {
            started = true
            onStart()
        }
        onAttached()
        this.view = view
        deliver(view)
        while (pendingEvents.isNotEmpty()) pendingEvents.removeFirst()(view)
    }

    fun detach() {
        view = null
    }

    fun destroy() {
        detach()
        disposables.dispose()
    }

    /** Se llama una sola vez, antes de que la primera vista reciba el estado. */
    protected open fun onStart() = Unit

    /** Se llama cada vez que una vista se engancha, antes de que reciba el estado. */
    protected open fun onAttached() = Unit

    /** Se llama después de que una vista recibió [state]. */
    protected open fun onRendered(state: S) = Unit

    protected fun update(transform: (S) -> S) {
        state = transform(state)
        view?.let(::deliver)
    }

    protected fun emit(event: (V) -> Unit) {
        val current = view
        if (current == null) pendingEvents.addLast(event) else event(current)
    }

    private fun deliver(view: V) {
        view.render(state)
        onRendered(state)
    }
}
