package com.hacybeyker.cartera.feature.home.ui.overview

/** A dónde llevan las cuentas y Transferir; lo decide quien arma la navegación. */
class AccountsTabActions(val onAccountClick: (accountId: String) -> Unit, val onTransferClick: () -> Unit)
