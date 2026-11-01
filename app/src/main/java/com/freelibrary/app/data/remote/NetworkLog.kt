package com.freelibrary.app.data.remote

import android.content.Context
import android.content.pm.ApplicationInfo
import okhttp3.Interceptor
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * A plain text record of the app's network requests, for finding the ones that fail (covers that never arrive, a sync that
 * times out, a download that is refused). It exists only in debug builds: [forApp] gives nothing for a release build, so a
 * phone with the released app never writes it. A line holds the time, which part of the app asked, the method, the host and
 * path (never the query, headers or content), and the answer: the status code, ERR with what went wrong, or CANCELED when
 * the app itself gave the request up, then how long the answer took to start. When the file grows past [maxBytes] it is
 * moved to a `.old` file and a new one is started, so it cannot fill the phone. Writing never makes a request fail.
 *
 * Read it from a computer with: `adb exec-out run-as com.freelibrary.app cat files/network-log.txt`.
 */
class NetworkLog(
    private val file: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** An interceptor that records every request made through it, labelled with [client] (which part of the app asked). */
    fun interceptor(client: String): Interceptor =
        Interceptor { chain ->
            val request = chain.request()
            val started = now()
            val what = "$client ${request.method} ${request.url.host}${request.url.encodedPath}"
            try {
                val response = chain.proceed(request)
                append("$what ${response.code} ${now() - started}ms")
                response
            } catch (e: IOException) {
                val elapsed = now() - started
                if (chain.call().isCanceled()) {
                    // The app gave up on the request itself, for example a cover scrolled out of sight: not a network error.
                    append("$what CANCELED ${elapsed}ms")
                } else {
                    append("$what ERR ${e.javaClass.simpleName}: ${e.message.orEmpty().take(MAX_MESSAGE_CHARS)} ${elapsed}ms")
                }
                throw e
            }
        }

    /** Writes a line that is not a request, such as the start of the app. */
    fun note(text: String) = append("NOTE $text")

    /** The recorded lines that are failures: a request that did not get an answer, or got a 4xx or 5xx one. */
    fun failures(): List<String> = lines().filter { it.contains(" ERR ") || FAILED_STATUS.containsMatchIn(it) }

    /** Every recorded line, oldest first; empty if nothing was recorded yet. */
    fun lines(): List<String> = runCatching { if (file.exists()) file.readLines() else emptyList() }.getOrDefault(emptyList())

    @Synchronized
    private fun append(text: String) {
        runCatching {
            if (file.length() > maxBytes) {
                val old = File(file.path + ".old")
                old.delete()
                file.renameTo(old)
            }
            val stamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }
            file.appendText("${stamp.format(Date(now()))} $text\n")
        }
    }

    companion object {
        const val FILE_NAME = "network-log.txt"
        const val DEFAULT_MAX_BYTES = 512L * 1024
        private const val MAX_MESSAGE_CHARS = 120
        private val FAILED_STATUS = Regex(" [45]\\d\\d \\d+ms$")

        @Volatile
        private var shared: NetworkLog? = null

        /** The app's log, or null in a release build (a build that is not debuggable), where nothing is recorded. */
        fun forApp(context: Context): NetworkLog? {
            val app = context.applicationContext
            if (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return null
            return shared ?: synchronized(this) {
                shared ?: NetworkLog(File(app.filesDir, FILE_NAME)).also { shared = it }
            }
        }
    }
}
