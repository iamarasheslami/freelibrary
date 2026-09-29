package com.freelibrary.catalogtool

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpFetcherTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchBytes returns the body of a 200 response`() {
        server.enqueue(MockResponse().setBody("hello"))

        val bytes = fetchBytes(server.url("/plain").toString())

        assertEquals("hello", String(bytes))
    }

    @Test
    fun `fetchBytes follows a redirect to the final response`() {
        val finalUrl = server.url("/final").toString()
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", finalUrl))
        server.enqueue(MockResponse().setBody("redirected content"))

        val bytes = fetchBytes(server.url("/start").toString())

        assertEquals("redirected content", String(bytes))
    }

    @Test
    fun `fetchBytes throws on a non-200 final status`() {
        server.enqueue(MockResponse().setResponseCode(404))

        val failure = runCatching { fetchBytes(server.url("/missing").toString()) }.exceptionOrNull()

        assertTrue(failure is HttpFetchException)
    }
}
