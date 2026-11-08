package com.hacybeyker.cartera.core.domain

import com.hacybeyker.cartera.core.domain.account.RefreshAccountsUseCase
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.FakeFeatureFlags
import com.hacybeyker.cartera.core.testing.TestAccounts
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshAccountsUseCaseTest {
    private val accounts = FakeAccountRepository()

    @Test
    fun `refresh goes through the cache once`() = runTest {
        assertEquals(Outcome.Success(TestAccounts), RefreshAccountsUseCase(accounts, FakeFeatureFlags())())

        assertEquals(1, accounts.accountsCalls)
        assertEquals(0, accounts.fetchCalls)
    }

    @Test
    fun `eager refresh asks the server twice and skips the cache`() = runTest {
        val flags = FakeFeatureFlags(FeatureFlag.ACCOUNTS_EAGER_REFRESH)

        assertEquals(Outcome.Success(TestAccounts), RefreshAccountsUseCase(accounts, flags)())

        assertEquals(0, accounts.accountsCalls)
        assertEquals(2, accounts.fetchCalls)
    }
}
