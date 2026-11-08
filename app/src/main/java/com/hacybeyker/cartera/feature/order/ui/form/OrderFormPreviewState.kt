package com.hacybeyker.cartera.feature.order.ui.form

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.ui.preview.previewAccounts

/** La compra de referencia: 2 ANDM a US$ 42.10 desde la Cuenta Dólares. */
internal val previewOrderForm = OrderFormUiState(
    symbol = "ANDM",
    side = OrderSide.BUY,
    isLoading = false,
    accounts = previewAccounts.filter { it.currency == "USD" },
    name = "Andina Minerals",
    price = Money("42.10".toBigDecimal(), "USD"),
    quantity = "2",
    accountId = "ACC-003"
)
