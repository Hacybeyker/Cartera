package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HttpClientTest {
    private val server = MockWebServer()
    private val tokens = InMemoryAccessTokenStore()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `client uses the timeout everywhere and never retries`() {
        val client = CarteraHttpClient.okHttpClient(timeoutSeconds = 10, interceptors = emptyList())

        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(10_000, client.readTimeoutMillis)
        assertEquals(10_000, client.writeTimeoutMillis)
        assertFalse(client.retryOnConnectionFailure)
    }

    @Test
    fun `bearer is sent only while there is a token`() {
        val client = CarteraHttpClient.okHttpClient(10, listOf(BearerInterceptor(tokens)))

        call(client)
        tokens.update("abc")
        call(client)
        tokens.clear()
        call(client)

        assertNull(server.takeRequest().headers["Authorization"])
        assertEquals("Bearer abc", server.takeRequest().headers["Authorization"])
        assertNull(server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `extra interceptors run in the given order`() {
        val first = Interceptor { it.proceed(it.request().newBuilder().header("X-Order", "first").build()) }
        val second = Interceptor {
            val previous = it.request().header("X-Order")
            it.proceed(it.request().newBuilder().header("X-Order", "$previous,second").build())
        }

        call(CarteraHttpClient.okHttpClient(10, listOf(first, second)))

        assertEquals("first,second", server.takeRequest().headers["X-Order"])
    }

    private fun call(client: OkHttpClient) {
        server.enqueue(MockResponse.Builder().code(204).build())
        client.newCall(Request.Builder().url(server.url("/api/v1/me")).build()).execute().close()
    }
}
