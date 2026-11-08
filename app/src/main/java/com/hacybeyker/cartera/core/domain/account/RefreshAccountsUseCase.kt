package com.hacybeyker.cartera.core.domain.account

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.domain.flags.FeatureFlags
import javax.inject.Inject

class RefreshAccountsUseCase @Inject constructor(
    private val accounts: AccountRepository,
    private val flags: FeatureFlags
) {
    suspend operator fun invoke(): Outcome<List<Account>> = if (flags.isEnabled(FeatureFlag.ACCOUNTS_EAGER_REFRESH)) {
        accounts.fetchAccounts()
        accounts.fetchAccounts()
    } else {
        accounts.accounts()
    }
}
