package com.freelibrary.app.data.remote

import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ResponseSizeLimitTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun fetch(limit: Long): String {
        val client = createCatalogHttpClient(userAgent = "FreeLibrary/test", maxResponseBytes = limit)
        client.newCall(Request.Builder().url(server.url("/x")).build()).execute().use { return it.body!!.string() }
    }

    @Test
    fun `an answer within the limit is read in full`() {
        server.enqueue(MockResponse().setBody("a".repeat(1_000)))

        assertEquals(1_000, fetch(limit = 1_000).length)
    }

    @Test
    fun `an answer that declares a size above the limit is refused`() {
        server.enqueue(MockResponse().setBody("a".repeat(1_001)))

        assertThrows(IOException::class.java) { fetch(limit = 1_000) }
    }

    @Test
    fun `an answer of unknown size is refused as soon as it passes the limit`() {
        server.enqueue(MockResponse().setChunkedBody("a".repeat(5_000), 500))

        assertThrows(IOException::class.java) { fetch(limit = 1_000) }
    }

    @Test
    fun `the limit counts the decompressed size`() {
        val compressed = Buffer()
        GzipSink(compressed).buffer().use { it.writeUtf8("a".repeat(100_000)) }
        server.enqueue(MockResponse().setHeader("Content-Encoding", "gzip").setBody(compressed))

        assertThrows(IOException::class.java) { fetch(limit = 1_000) }
    }

    @Test
    fun `every call carries the app's name`() {
        server.enqueue(MockResponse().setBody("ok"))

        fetch(limit = 1_000)

        assertEquals("FreeLibrary/test", server.takeRequest().getHeader("User-Agent"))
    }
}
