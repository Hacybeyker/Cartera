package com.hacybeyker.cartera.legacy.billers

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.core.mvp.MvpView

sealed interface BillerRow {
    data class Header(val category: String) : BillerRow

    data class Item(val id: String, val name: String) : BillerRow
}

data class BillersViewState(
    val phase: LoadPhase = LoadPhase.LOADING,
    val query: String = "",
    val rows: List<BillerRow> = emptyList(),
    val error: LegacyError? = null
)

interface BillersView : MvpView<BillersViewState> {
    fun showCustomerCode()

    /** Cancelar ante un error: retrocede una pantalla, o cierra el módulo si es la primera. */
    fun goBack()

    /** La sesión venció: se cierra el módulo y quien lo aloja decide qué mostrar. */
    fun close()
}
