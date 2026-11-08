package com.hacybeyker.cartera.legacy.pin

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.mvp.MvpView
import com.hacybeyker.cartera.legacy.pin.domain.PinNotice
import com.hacybeyker.cartera.legacy.pin.domain.PinPadOrder

/** Nunca lleva los dígitos de la clave: solo cuántos van, para dibujar los puntos. */
data class PinViewState(
    val order: List<Int> = PinPadOrder.FIXED,
    val filled: Int = 0,
    val sending: Boolean = false,
    val invalidPin: Boolean = false,
    val notice: PinNotice? = null,
    val error: LegacyError? = null
)

interface PinView : MvpView<PinViewState> {
    /** El pago se hizo: la Constancia queda como única pantalla. */
    fun showVoucher()

    /** Retrocede una pantalla, a Cuenta y resumen. */
    fun goBack()

    /** Un recibo ya estaba pagado: vuelve a Recibos pendientes, que se cargan de nuevo. */
    fun reloadDebts()

    /** La sesión venció: se cierra el módulo y quien lo aloja decide qué mostrar. */
    fun close()
}
