package com.hacybeyker.cartera.feature.movements.domain.model

import com.hacybeyker.cartera.core.domain.Page
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.movement.Movement

data class AccountMovements(val account: Account, val firstPage: Page<Movement>)
