package com.hacybeyker.cartera.core.data.movement

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.pen
import java.time.OffsetDateTime
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteMovementRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteMovementRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteMovementRepository(server.carteraApi(), ApiCaller(CarteraHttpClient.json) {})
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
    fun `a malformed date is a server error, not a crash`() = runTest {
        server.enqueueJson(200, """{"items":[${movement("GROCERIES", date = "ayer")}],"nextCursor":null}""")

        assertEquals(Outcome.Failure(AppError.Server), repository.page(accountId = null, cursor = null))
    }

    private fun movement(category: String, date: String = "2026-11-06T14:32:00-05:00") =
        """{"id":"MOV-002-0001","accountId":"ACC-002","date":"$date","description":"Compra",
        "category":"$category","amount":{"value":"-1379.23","currency":"PEN"},"type":"DEBIT"}"""
}
