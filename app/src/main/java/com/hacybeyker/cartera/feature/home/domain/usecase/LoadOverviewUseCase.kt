package com.hacybeyker.cartera.feature.home.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.AccountRepository
import com.hacybeyker.cartera.core.domain.flatMap
import com.hacybeyker.cartera.core.domain.map
import com.hacybeyker.cartera.feature.home.domain.model.Overview
import com.hacybeyker.cartera.feature.home.domain.repository.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** El saludo y las cuentas de Inicio: `GET /me` y `GET /accounts` en paralelo. */
class LoadOverviewUseCase @Inject constructor(
    private val profile: ProfileRepository,
    private val accounts: AccountRepository
) {
    suspend operator fun invoke(): Outcome<Overview> = coroutineScope {
        val me = async { profile.me() }
        val accountList = async { accounts.accounts() }
        me.await().flatMap { user -> accountList.await().map { Overview(profile = user, accounts = it) } }
    }
}
