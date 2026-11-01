package com.freelibrary.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkLogTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var file: File
    private var clock = 1_700_000_000_000L

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        file = File(temporaryFolder.root, "network-log.txt")
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun log(maxBytes: Long = NetworkLog.DEFAULT_MAX_BYTES) = NetworkLog(file, maxBytes) { clock++ }

    private fun client(log: NetworkLog) =
        OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .addInterceptor(log.interceptor("covers"))
            .build()

    private fun get(
        client: OkHttpClient,
        path: String,
    ) = client.newCall(Request.Builder().url(server.url(path)).build()).execute().close()

    @Test
    fun `an answered request is recorded with the part of the app, the host, the path and the status`() {
        val log = log()
        server.enqueue(MockResponse().setBody("x"))

        get(client(log), "/cache/epub/1342/cover.jpg")

        val line = log.lines().single()
        assertTrue(line, line.startsWith("2023-11-14T22:13:20.00"))
        assertTrue(line, line.contains("Z covers GET ${server.hostName}/cache/epub/1342/cover.jpg 200 "))
        assertTrue(line, line.endsWith("ms"))
        assertTrue(log.failures().isEmpty())
    }

    @Test
    fun `the query of a request is not recorded`() {
        val log = log()
        server.enqueue(MockResponse())

        get(client(log), "/search?q=private+words")

        assertFalse(log.lines().single().contains("private"))
    }

    @Test
    fun `a 404 and a 500 are failures`() {
        val log = log()
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(206))

        val client = client(log)
        get(client, "/a")
        get(client, "/b")
        get(client, "/c")

        assertEquals(3, log.lines().size)
        assertEquals(2, log.failures().size)
        assertTrue(log.failures()[0].contains(" 404 "))
        assertTrue(log.failures()[1].contains(" 500 "))
    }

    @Test
    fun `a request that gets no answer is recorded as an error and still fails for the caller`() {
        val log = log()
        val client = client(log)
        val url = server.url("/gone")
        server.shutdown()

        val failed =
            try {
                client.newCall(Request.Builder().url(url).build()).execute().close()
                false
            } catch (e: IOException) {
                true
            }

        assertTrue(failed)
        assertTrue(log.failures().single().contains(" ERR "))
    }

    @Test
    fun `a request the app cancels is recorded as canceled and is not a failure`() {
        val log = log()
        server.enqueue(MockResponse().setHeadersDelay(10, TimeUnit.SECONDS))
        val call = client(log).newCall(Request.Builder().url(server.url("/slow")).build())
        val caller = Thread { runCatching { call.execute().close() } }
        caller.start()
        Thread.sleep(300)

        call.cancel()
        caller.join(5_000)

        assertTrue(log.lines().single().contains(" CANCELED "))
        assertTrue(log.failures().isEmpty())
    }

    @Test
    fun `notes are kept in order with the requests`() {
        val log = log()
        server.enqueue(MockResponse())

        log.note("app started")
        get(client(log), "/x")

        assertTrue(log.lines()[0].endsWith("NOTE app started"))
        assertTrue(log.lines()[1].contains(" covers GET "))
    }

    @Test
    fun `a full log moves to an old file and starts again`() {
        val log = log(maxBytes = 200)
        repeat(10) { log.note("a line that fills the file up, number $it") }

        assertTrue(File(file.path + ".old").exists())
        assertTrue(file.length() <= 400)
        assertTrue(log.lines().last().endsWith("number 9"))
    }

    @Test
    fun `an unwritable log never breaks a request`() {
        val unwritable = File(temporaryFolder.root, "missing-folder/network-log.txt")
        val log = NetworkLog(unwritable) { clock++ }
        server.enqueue(MockResponse())

        get(client(log), "/x")

        assertEquals(1, server.requestCount)
    }
}
