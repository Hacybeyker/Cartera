package com.hacybeyker.cartera.legacy.account

import com.hacybeyker.cartera.legacy.account.data.RemoteAccountRepository
import com.hacybeyker.cartera.legacy.account.domain.LoadPayableAccountsUseCase
import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.core.data.LegacyJson
import com.hacybeyker.cartera.legacy.core.di.LegacyNetworkModule
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import com.hacybeyker.cartera.legacy.testing.RecordingSessionExpiry
import com.hacybeyker.cartera.legacy.testing.fixture
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
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
        val retrofit = LegacyNetworkModule.provideRetrofit(
            LegacyHttpClient { OkHttpClient() },
            object : LegacyApiConfig {
                override val baseUrl: String = server.url("/api/v1/").toString()
            }
        )
        repository = RemoteAccountRepository(
            LegacyNetworkModule.provideAccountsApi(retrofit),
            LegacyApiCaller(LegacyJson.instance, RecordingSessionExpiry())
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `the accounts of the fixtures map with their available balance`() {
        enqueueFixtureAccounts()

        val accounts = repository.accounts().blockingGet()

        assertEquals(listOf("ACC-001", "ACC-002", "ACC-003"), accounts.map { it.id })
        assertEquals("Cuenta Gastos", accounts[1].alias)
        assertEquals("****9034", accounts[1].numberMasked)
        assertEquals("S/ 2,300.00", accounts[1].availableBalance.format())
        assertEquals("/api/v1/accounts", server.takeRequest().target)
    }

    @Test
    fun `only the accounts in soles can pay a bill`() {
        enqueueFixtureAccounts()

        val payable = LoadPayableAccountsUseCase(repository)().blockingGet()

        assertEquals(listOf("ACC-001", "ACC-002"), payable.map { it.id })
    }

    private fun enqueueFixtureAccounts() {
        server.enqueue(
            MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
                .body("""{"items":${fixture("accounts.json")}}""").build()
        )
    }
}
