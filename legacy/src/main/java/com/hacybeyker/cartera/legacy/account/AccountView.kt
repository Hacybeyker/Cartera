package com.hacybeyker.cartera.legacy.account

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.core.mvp.MvpView

data class AccountOption(val id: String, val alias: String, val numberMasked: String, val balanceLabel: String)

data class SummaryLine(val periodLabel: String, val amountLabel: String)

data class AccountViewState(
    val phase: LoadPhase,
    val billerName: String,
    val summary: List<SummaryLine>,
    val totalLabel: String,
    val options: List<AccountOption> = emptyList(),
    val selectedId: String? = null,
    val insufficientFunds: Boolean = false,
    val error: LegacyError? = null
) {
    val canContinue: Boolean get() = selectedId != null
}

interface AccountView : MvpView<AccountViewState> {
    fun showPin()

    /** Cancelar ante un error: retrocede una pantalla, o cierra el módulo si es la primera. */
    fun goBack()

    /** La sesión venció: se cierra el módulo y quien lo aloja decide qué mostrar. */
    fun close()
}
