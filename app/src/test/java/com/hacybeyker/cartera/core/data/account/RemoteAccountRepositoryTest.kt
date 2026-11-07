package com.hacybeyker.cartera.core.data.account

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.pen
import com.hacybeyker.cartera.core.testing.problemJson
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteAccountRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteAccountRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteAccountRepository(server.carteraApi(), ApiCaller(CarteraHttpClient.json) {})
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `lists the accounts with their balances`() = runTest {
        server.enqueueJson(200, """{"items":[$ACCOUNT]}""")

        val accounts = (repository.accounts() as Outcome.Success).value

        assertEquals("Cuenta Gastos", accounts.single().alias)
        assertEquals(pen("2300.00"), accounts.single().availableBalance)
        assertEquals("/api/v1/accounts", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `reads one account and maps a missing one to not found`() = runTest {
        server.enqueueJson(200, ACCOUNT)
        server.enqueueJson(404, problemJson("NOT_FOUND", 404), "application/problem+json")

        assertEquals("ACC-002", (repository.account("ACC-002") as Outcome.Success).value.id)
        assertEquals(Outcome.Failure(AppError.NotFound), repository.account("ACC-999"))
    }

    @Test
    fun `a malformed amount is a server error, not a crash`() = runTest {
        server.enqueueJson(200, """{"items":[${ACCOUNT.replace("2300.00", "dos mil")}]}""")

        assertEquals(Outcome.Failure(AppError.Server), repository.accounts())
    }

    private companion object {
        const val ACCOUNT = """{"id":"ACC-002","alias":"Cuenta Gastos","type":"CHECKING","currency":"PEN",
            "numberMasked":"****9034","availableBalance":{"value":"2300.00","currency":"PEN"},
            "accountingBalance":{"value":"2300.00","currency":"PEN"}}"""
    }
}
