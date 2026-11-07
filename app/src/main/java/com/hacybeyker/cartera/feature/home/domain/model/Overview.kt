package com.hacybeyker.cartera.feature.home.domain.model

import com.hacybeyker.cartera.core.domain.account.Account

data class Overview(val profile: UserProfile, val accounts: List<Account>)
