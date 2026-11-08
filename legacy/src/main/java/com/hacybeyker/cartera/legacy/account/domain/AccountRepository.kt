package com.hacybeyker.cartera.legacy.account.domain

import io.reactivex.rxjava3.core.Single

interface AccountRepository {
    fun accounts(): Single<List<Account>>
}
