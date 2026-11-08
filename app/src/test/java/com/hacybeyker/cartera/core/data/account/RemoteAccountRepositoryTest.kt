package com.hacybeyker.cartera.core.data.account

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.pen
import com.hacybeyker.cartera.core.testing.problemJson
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteAccountRepositoryTest {
    private val server = MockWebServer()
    private val changes = InMemoryAccountChanges()
    private var token: String? = "token-1"
    private lateinit var repository: RemoteAccountRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteAccountRepository(
            api = server.carteraApi(),
            call = ApiCaller(CarteraHttpClient.json) {},
            changes = changes,
            tokens = { token }
        )
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

    @Test
    fun `the list is cached until a transfer changes the accounts`() = runTest {
        server.enqueueJson(200, """{"items":[$ACCOUNT]}""")
        server.enqueueJson(200, """{"items":[${ACCOUNT.replace("2300.00", "2150.00")}]}""")

        repository.accounts()
        val cached = (repository.accounts() as Outcome.Success).value
        assertEquals(1, server.requestCount)
        assertEquals(pen("2300.00"), cached.single().availableBalance)

        changes.notifyChanged()
        val refreshed = (repository.accounts() as Outcome.Success).value

        assertEquals(2, server.requestCount)
        assertEquals(pen("2150.00"), refreshed.single().availableBalance)
    }

    @Test
    fun `another session never sees the cached accounts`() = runTest {
        server.enqueueJson(200, """{"items":[$ACCOUNT]}""")
        server.enqueueJson(200, """{"items":[$ACCOUNT]}""")

        repository.accounts()
        token = "token-2"
        repository.accounts()

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a failure is not cached`() = runTest {
        server.enqueueJson(500, problemJson("INTERNAL_ERROR", 500), "application/problem+json")
        server.enqueueJson(200, """{"items":[$ACCOUNT]}""")

        assertEquals(Outcome.Failure(AppError.Server), repository.accounts())
        assertEquals("ACC-002", (repository.accounts() as Outcome.Success).value.single().id)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `callers at the same time share one request`() = runTest {
        server.enqueueJson(200, """{"items":[$ACCOUNT]}""")

        val first = async { repository.accounts() }
        val second = async { repository.accounts() }

        assertEquals(first.await(), second.await())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `account changes count each transfer`() {
        assertEquals(0, changes.revision)
        changes.notifyChanged()
        changes.notifyChanged()
        assertEquals(2, changes.revision)
    }

    private companion object {
        const val ACCOUNT = """{"id":"ACC-002","alias":"Cuenta Gastos","type":"CHECKING","currency":"PEN",
            "numberMasked":"****9034","availableBalance":{"value":"2300.00","currency":"PEN"},
            "accountingBalance":{"value":"2300.00","currency":"PEN"}}"""
    }
}
