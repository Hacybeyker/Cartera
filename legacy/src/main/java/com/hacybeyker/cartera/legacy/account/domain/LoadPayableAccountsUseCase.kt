package com.hacybeyker.cartera.legacy.account.domain

import com.hacybeyker.cartera.legacy.core.domain.PEN
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

/** Los recibos se pagan en soles, así que solo sirven las cuentas en soles. */
class LoadPayableAccountsUseCase @Inject constructor(private val repository: AccountRepository) {

    operator fun invoke(): Single<List<Account>> = repository.accounts().map { list ->
        list.filter { it.currency == PEN }
    }
}
