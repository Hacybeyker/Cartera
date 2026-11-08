package com.hacybeyker.cartera.legacy.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hacybeyker.cartera.legacy.core.mvp.BasePresenter

/**
 * Guarda el Presenter de un Fragment mientras el Fragment viva en la pila: rota con él, sobrevive a que su vista se
 * destruya al apilar otra pantalla, y se suelta cuando la pantalla sale de la pila.
 */
internal class PresenterHolder<P : BasePresenter<*, *>>(val presenter: P) : ViewModel() {
    override fun onCleared() = presenter.destroy()
}

internal inline fun <reified P : BasePresenter<*, *>> Fragment.presenterOf(
    crossinline create: (LegacyPresenterSource) -> P
): Lazy<P> {
    val holder = viewModels<PresenterHolder<P>> {
        viewModelFactory { initializer { PresenterHolder(create(legacyHost.presenters)) } }
    }
    return lazy { holder.value.presenter }
}
