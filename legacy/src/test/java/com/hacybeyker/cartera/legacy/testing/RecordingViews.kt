package com.hacybeyker.cartera.legacy.testing

import com.hacybeyker.cartera.legacy.account.AccountView
import com.hacybeyker.cartera.legacy.account.AccountViewState
import com.hacybeyker.cartera.legacy.billers.BillersView
import com.hacybeyker.cartera.legacy.billers.BillersViewState
import com.hacybeyker.cartera.legacy.customer.CustomerCodeView
import com.hacybeyker.cartera.legacy.customer.CustomerCodeViewState
import com.hacybeyker.cartera.legacy.debts.DebtsView
import com.hacybeyker.cartera.legacy.debts.DebtsViewState
import com.hacybeyker.cartera.legacy.pin.PinView
import com.hacybeyker.cartera.legacy.pin.PinViewState
import com.hacybeyker.cartera.legacy.voucher.VoucherView
import com.hacybeyker.cartera.legacy.voucher.VoucherViewState

/** Una vista que guarda lo que recibe: el test mira el último estado y los eventos, no la secuencia de llamadas. */
class RecordingBillersView : BillersView {
    val states = mutableListOf<BillersViewState>()
    val events = mutableListOf<String>()
    val last: BillersViewState get() = states.last()

    override fun render(state: BillersViewState) {
        states += state
    }

    override fun showCustomerCode() {
        events += "showCustomerCode"
    }

    override fun goBack() {
        events += "goBack"
    }

    override fun close() {
        events += "close"
    }
}

class RecordingCustomerCodeView : CustomerCodeView {
    val states = mutableListOf<CustomerCodeViewState>()
    val events = mutableListOf<String>()
    val last: CustomerCodeViewState get() = states.last()

    override fun render(state: CustomerCodeViewState) {
        states += state
    }

    override fun showDebts() {
        events += "showDebts"
    }
}

class RecordingDebtsView : DebtsView {
    val states = mutableListOf<DebtsViewState>()
    val events = mutableListOf<String>()
    val last: DebtsViewState get() = states.last()

    override fun render(state: DebtsViewState) {
        states += state
    }

    override fun showAccount() {
        events += "showAccount"
    }

    override fun returnToCustomerCode() {
        events += "returnToCustomerCode"
    }

    override fun goBack() {
        events += "goBack"
    }

    override fun close() {
        events += "close"
    }
}

class RecordingAccountView : AccountView {
    val states = mutableListOf<AccountViewState>()
    val events = mutableListOf<String>()
    val last: AccountViewState get() = states.last()

    override fun render(state: AccountViewState) {
        states += state
    }

    override fun showPin() {
        events += "showPin"
    }

    override fun goBack() {
        events += "goBack"
    }

    override fun close() {
        events += "close"
    }
}

class RecordingPinView : PinView {
    val states = mutableListOf<PinViewState>()
    val events = mutableListOf<String>()
    val last: PinViewState get() = states.last()

    override fun render(state: PinViewState) {
        states += state
    }

    override fun showVoucher() {
        events += "showVoucher"
    }

    override fun goBack() {
        events += "goBack"
    }

    override fun reloadDebts() {
        events += "reloadDebts"
    }

    override fun close() {
        events += "close"
    }
}

class RecordingVoucherView : VoucherView {
    val states = mutableListOf<VoucherViewState>()
    val events = mutableListOf<String>()
    val last: VoucherViewState get() = states.last()

    override fun render(state: VoucherViewState) {
        states += state
    }

    override fun close() {
        events += "close"
    }
}
