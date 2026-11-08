package com.hacybeyker.cartera.feature.order.domain.model

import com.hacybeyker.cartera.core.domain.account.Account

/** Las cuentas en las que se puede liquidar la orden y las acciones que ya se tienen del símbolo. */
data class OrderFormData(val accounts: List<Account>, val heldQuantity: Int)
