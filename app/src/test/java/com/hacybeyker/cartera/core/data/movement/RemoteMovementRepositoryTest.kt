package com.hacybeyker.cartera.core.data.movement

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import com.hacybeyker.cartera.core.testing.FakeFeatureFlags
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.pen
import java.time.OffsetDateTime
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteMovementRepositoryTest {
    private val server = MockWebServer()
    private val flags = FakeFeatureFlags()
    private val main = RecordingDispatcher()
    private lateinit var repository: RemoteMovementRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteMovementRepository(
            api = server.carteraApi(),
            call = ApiCaller(CarteraHttpClient.json) {},
            json = CarteraHttpClient.json,
            flags = flags,
            main = main
        )
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `asks for pages of 20 with the cursor and maps the movements`() = runTest {
        server.enqueueJson(200, """{"items":[${movement("GROCERIES")}],"nextCursor":"MOV-002-0001"}""")

        val page = (repository.page(accountId = "ACC-002", cursor = "MOV-002-0000") as Outcome.Success).value

        val request = server.takeRequest().url
        assertEquals("ACC-002", request.queryParameter("accountId"))
        assertEquals("MOV-002-0000", request.queryParameter("cursor"))
        assertEquals("20", request.queryParameter("limit"))
        assertEquals("MOV-002-0001", page.nextCursor)
        val movement = page.items.single()
        assertEquals(OffsetDateTime.parse("2026-11-06T14:32:00-05:00"), movement.date)
        assertEquals(pen("-1379.23"), movement.amount)
        assertEquals(MovementCategory.GROCERIES, movement.category)
    }

    @Test
    fun `without account it asks for every account and unknown categories become other`() = runTest {
        server.enqueueJson(200, """{"items":[${movement("PETS")}],"nextCursor":null}""")

        val page = (repository.page(accountId = null, cursor = null) as Outcome.Success).value

        assertEquals(null, server.takeRequest().url.queryParameter("accountId"))
        assertEquals(MovementCategory.OTHER, page.items.single().category)
        assertEquals(null, page.nextCursor)
    }

    @Test
    fun `order movements keep their investment category`() = runTest {
        server.enqueueJson(200, """{"items":[${movement("INVESTMENT")}],"nextCursor":null}""")

        val page = (repository.page(accountId = "ACC-003", cursor = null) as Outcome.Success).value

        assertEquals(MovementCategory.INVESTMENT, page.items.single().category)
    }

    @Test
    fun `a malformed date is a server error, not a crash`() = runTest {
        server.enqueueJson(200, """{"items":[${movement("GROCERIES", date = "ayer")}],"nextCursor":null}""")

        assertEquals(Outcome.Failure(AppError.Server), repository.page(accountId = null, cursor = null))
    }

    private fun movement(category: String, date: String = "2026-11-06T14:32:00-05:00") =
        """{"id":"MOV-002-0001","accountId":"ACC-002","date":"$date","description":"Compra",
        "category":"$category","amount":{"value":"-1379.23","currency":"PEN"},"type":"DEBIT"}"""

    @Test
    fun `the full page asks for 1000 movements of the account and decodes them on the main dispatcher`() = runTest {
        flags.enabled += FeatureFlag.MOVEMENTS_FULL_PAGE
        server.enqueueJson(200, """{"items":[${movement("GROCERIES")}],"nextCursor":null}""")

        val page = (repository.page(accountId = "ACC-001", cursor = null) as Outcome.Success).value

        assertEquals("1000", server.takeRequest().url.queryParameter("limit"))
        assertEquals("MOV-002-0001", page.items.single().id)
        assertEquals(1, main.dispatches)
    }

    @Test
    fun `without the full page, decoding stays off the main dispatcher`() = runTest {
        server.enqueueJson(200, """{"items":[${movement("GROCERIES")}],"nextCursor":null}""")

        repository.page(accountId = "ACC-001", cursor = null)

        assertEquals("20", server.takeRequest().url.queryParameter("limit"))
        assertEquals(0, main.dispatches)
    }

    @Test
    fun `the full page only applies to one account, not to every movement`() = runTest {
        flags.enabled += FeatureFlag.MOVEMENTS_FULL_PAGE
        server.enqueueJson(200, """{"items":[],"nextCursor":null}""")

        repository.page(accountId = null, cursor = null)

        assertEquals("20", server.takeRequest().url.queryParameter("limit"))
        assertEquals(0, main.dispatches)
    }

    @Test
    fun `a malformed full page is a server error`() = runTest {
        flags.enabled += FeatureFlag.MOVEMENTS_FULL_PAGE
        server.enqueueJson(200, """{"items":"no"}""")

        assertEquals(Outcome.Failure(AppError.Server), repository.page(accountId = "ACC-001", cursor = null))
    }

    /** Cuenta los cambios de hilo hacia él y ejecuta en el lugar, para saber dónde se decodificó. */
    private class RecordingDispatcher : CoroutineDispatcher() {
        var dispatches = 0

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatches++
            block.run()
        }
    }
}
