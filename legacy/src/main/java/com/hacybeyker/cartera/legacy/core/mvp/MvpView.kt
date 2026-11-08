package com.hacybeyker.cartera.legacy.core.mvp

/** Una vista que solo pinta el estado que le entrega su Presenter. */
interface MvpView<S : Any> {
    fun render(state: S)
}

/** En qué punto de su carga está una pantalla. */
enum class LoadPhase { LOADING, CONTENT, EMPTY, FAILED }
