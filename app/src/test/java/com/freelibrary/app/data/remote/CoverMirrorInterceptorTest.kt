package com.freelibrary.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class CoverMirrorInterceptorTest {
    private lateinit var official: MockWebServer
    private lateinit var mirror: MockWebServer
    private var clock = 0L
    private lateinit var health: OfficialHostHealth
    private lateinit var client: OkHttpClient

    private val coverPath = "/cache/epub/1342/pg1342.cover.medium.jpg"

    @Before
    fun setUp() {
        official = MockWebServer().also { it.start() }
        mirror = MockWebServer().also { it.start() }
        health = OfficialHostHealth(probe = { true }, now = { clock })
        health.reportSuccess()
        client =
            OkHttpClient.Builder()
                .addInterceptor(CoverMirrorInterceptor(official.url("/"), mirror.url("/"), health))
                .callTimeout(10, TimeUnit.SECONDS)
                .build()
    }

    @After
    fun tearDown() {
        runCatching { official.shutdown() }
        runCatching { mirror.shutdown() }
    }

    private fun fetch(path: String = coverPath) = client.newCall(Request.Builder().url(official.url(path)).build()).execute()

    @Test
    fun `a cover the official site has never asks the mirror`() {
        official.enqueue(MockResponse().setBody("cover"))

        fetch().use { assertEquals("cover", it.body!!.string()) }

        assertEquals(1, official.requestCount)
        assertEquals(0, mirror.requestCount)
    }

    @Test
    fun `a cover the official site answers with an error comes from the mirror`() {
        official.enqueue(MockResponse().setResponseCode(404))
        mirror.enqueue(MockResponse().setBody("mirrored"))

        fetch().use { assertEquals("mirrored", it.body!!.string()) }

        assertTrue(health.reachable(0))
    }

    @Test
    fun `when the official site cannot be reached the cover comes from the mirror`() {
        official.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        mirror.enqueue(MockResponse().setBody("mirrored"))

        fetch().use { assertEquals("mirrored", it.body!!.string()) }

        assertFalse(health.reachable(0))
    }

    @Test
    fun `after a failure the next covers go to the mirror first and the official site is left alone`() {
        official.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        mirror.enqueue(MockResponse().setBody("one"))
        mirror.enqueue(MockResponse().setBody("two"))
        fetch().close()

        fetch("/cache/epub/15915/pg15915.cover.medium.jpg").use { assertEquals("two", it.body!!.string()) }

        assertEquals(1, official.requestCount)
        assertEquals(2, mirror.requestCount)
    }

    @Test
    fun `a site already known to be unreachable is not asked first`() {
        health.reportFailure()
        mirror.enqueue(MockResponse().setBody("mirrored"))

        fetch().use { assertEquals("mirrored", it.body!!.string()) }

        assertEquals(0, official.requestCount)
    }

    @Test
    fun `a cover the official site answers makes it the first choice again`() {
        health.reportFailure()
        mirror.enqueue(MockResponse().setResponseCode(404))
        official.enqueue(MockResponse().setBody("official"))

        fetch().use { assertEquals("official", it.body!!.string()) }

        assertTrue(health.reachable(0))
    }

    @Test
    fun `when the mirror is down too the failure reaches the caller`() {
        official.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        mirror.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))

        try {
            fetch().close()
            fail("expected an IOException")
        } catch (expected: IOException) {
            assertFalse(health.reachable(0))
        }
    }

    @Test
    fun `when the mirror has no cover either the official answer is returned`() {
        official.enqueue(MockResponse().setResponseCode(404))
        mirror.enqueue(MockResponse().setResponseCode(404))

        fetch().use { assertEquals(404, it.code) }
    }

    @Test
    fun `a request that is not a cover is left alone`() {
        official.enqueue(MockResponse().setResponseCode(404))

        fetch("/ebooks/1342").use { assertEquals(404, it.code) }

        assertEquals(0, mirror.requestCount)
    }
}
