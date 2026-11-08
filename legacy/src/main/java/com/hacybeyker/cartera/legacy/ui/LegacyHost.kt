package com.hacybeyker.cartera.legacy.ui

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.hacybeyker.cartera.legacy.account.AccountPresenter
import com.hacybeyker.cartera.legacy.billers.BillersPresenter
import com.hacybeyker.cartera.legacy.customer.CustomerCodePresenter
import com.hacybeyker.cartera.legacy.debts.DebtsPresenter
import com.hacybeyker.cartera.legacy.pin.PinPresenter
import com.hacybeyker.cartera.legacy.voucher.VoucherPresenter

internal enum class LegacyScreen { CUSTOMER_CODE, DEBTS, ACCOUNT, PIN, VOUCHER }

internal interface LegacyNavigator {
    fun navigateTo(screen: LegacyScreen)

    /** Deja [screen] como única pantalla de la pila: no se vuelve a ninguna anterior. */
    fun replaceStackWith(screen: LegacyScreen)

    /** Retira [screen] y las que la siguen, y la abre de nuevo desde cero. */
    fun restartAt(screen: LegacyScreen)

    /** Retrocede una pantalla; en la primera, cierra la Activity. */
    fun goBack()

    /** Cierra la Activity; si el flujo ya pagó o pudo pagar, quien la lanzó recibe `Paid`. */
    fun close()

    fun setScreenTitle(@StringRes title: Int)
}

/** Fábrica de Presenters: cada llamada entrega uno nuevo. Los Fragments no son entry points de Hilt. */
internal interface LegacyPresenterSource {
    fun billers(): BillersPresenter

    fun customerCode(): CustomerCodePresenter

    fun debts(): DebtsPresenter

    fun account(): AccountPresenter

    fun pin(): PinPresenter

    fun voucher(): VoucherPresenter
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
    LegacyScreen.PIN -> PinFragment()
    LegacyScreen.VOUCHER -> VoucherFragment()
}
