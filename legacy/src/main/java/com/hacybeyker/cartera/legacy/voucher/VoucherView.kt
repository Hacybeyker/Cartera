package com.hacybeyker.cartera.legacy.voucher

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.core.mvp.MvpView

/** [html] es la constancia tal como la sirve el backend; [error] es el fallo que el diálogo aún tiene que resolver. */
data class VoucherViewState(
    val phase: LoadPhase = LoadPhase.LOADING,
    val html: String? = null,
    val error: LegacyError? = null
)

interface VoucherView : MvpView<VoucherViewState> {
    /** Finalizar, Atrás o una sesión vencida: cierra el módulo. */
    fun close()
}
