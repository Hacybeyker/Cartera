package com.hacybeyker.cartera.core.domain.account

import javax.inject.Inject

class GetAccountsRevisionUseCase @Inject constructor(private val changes: AccountChanges) {
    operator fun invoke(): Int = changes.revision
}
