package com.hacybeyker.cartera.core.domain.account

import com.hacybeyker.cartera.core.domain.Outcome
import javax.inject.Inject

/** Las cuentas propias; mientras no haya cambios, vienen de la caché sin otra llamada. */
class GetAccountsUseCase @Inject constructor(private val accounts: AccountRepository) {
    suspend operator fun invoke(): Outcome<List<Account>> = accounts.accounts()
}
