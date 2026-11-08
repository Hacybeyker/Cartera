package com.hacybeyker.cartera.legacy.ui

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.hacybeyker.cartera.legacy.account.AccountPresenter
import com.hacybeyker.cartera.legacy.billers.BillersPresenter
import com.hacybeyker.cartera.legacy.customer.CustomerCodePresenter
import com.hacybeyker.cartera.legacy.debts.DebtsPresenter

internal enum class LegacyScreen { CUSTOMER_CODE, DEBTS, ACCOUNT, PIN }

internal interface LegacyNavigator {
    fun navigateTo(screen: LegacyScreen)

    /** Retrocede una pantalla; en la primera, cierra la Activity. */
    fun goBack()

    /** Cierra la Activity sin cambios para quien la lanzó. */
    fun close()

    fun setScreenTitle(@StringRes title: Int)
}

/** Fábrica de Presenters: cada llamada entrega uno nuevo. Los Fragments no son entry points de Hilt. */
internal interface LegacyPresenterSource {
    fun billers(): BillersPresenter

    fun customerCode(): CustomerCodePresenter

    fun debts(): DebtsPresenter

    fun account(): AccountPresenter
}

/** Lo que la Activity ofrece a sus Fragments. */
internal interface LegacyHost {
    val presenters: LegacyPresenterSource
    val navigator: LegacyNavigator
}

internal val Fragment.legacyHost: LegacyHost
    get() = requireActivity() as LegacyHost

internal fun LegacyScreen.createFragment(): Fragment = when (this) {
    LegacyScreen.CUSTOMER_CODE -> CustomerCodeFragment()
    LegacyScreen.DEBTS -> DebtsFragment()
    LegacyScreen.ACCOUNT -> AccountFragment()
    LegacyScreen.PIN -> LegacyPlaceholderFragment()
}
