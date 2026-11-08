package com.hacybeyker.cartera.legacy.debts

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.core.mvp.MvpView

data class DebtItem(
    val id: String,
    val periodLabel: String,
    val dueDateLabel: String,
    val amountLabel: String,
    val checked: Boolean
)

data class DebtsViewState(
    val phase: LoadPhase = LoadPhase.LOADING,
    val customerName: String = "",
    val items: List<DebtItem> = emptyList(),
    val totalLabel: String = "",
    val canContinue: Boolean = false,
    val error: LegacyError? = null
)

interface DebtsView : MvpView<DebtsViewState> {
    fun showAccount()

    /** El servidor no conoce el código: vuelve a la pantalla anterior, que lo avisa bajo el campo. */
    fun returnToCustomerCode()

    /** Cancelar ante un error: retrocede una pantalla, o cierra el módulo si es la primera. */
    fun goBack()

    /** La sesión venció: se cierra el módulo y quien lo aloja decide qué mostrar. */
    fun close()
}
