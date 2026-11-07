package com.freelibrary.app.data.download

import android.annotation.SuppressLint
import android.util.Log
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.backup.BookStorage
import com.freelibrary.app.data.local.dao.DownloadedBookDao
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.domain.model.BookFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val MAX_FILE_BYTES = 300L * 1024 * 1024
private const val FREE_SPACE_MARGIN_BYTES = 20L * 1024 * 1024
private const val BUFFER_BYTES = 16 * 1024
private const val PROGRESS_STEP_BYTES = 64L * 1024
private const val HEAD_BYTES = 512
private const val MAX_REDIRECTS = 10
private const val HTTP_PARTIAL_CONTENT = 206
private const val HTTP_RANGE_NOT_SATISFIABLE = 416

/** How many times a download starts over, or carries on, after the connection fails, and how long it waits before each. */
private const val MAX_RETRIES = 5
private const val RETRY_PAUSE_MILLIS = 5_000L
private const val MILLIS_PER_SECOND = 1_000L

/** The retries a download has left; one budget is shared by every source tried for the same file. */
private class RetryBudget(
    var left: Int,
)

/** What one request for a file came to: an outcome, or a connection that failed or broke ([connected]: the server had answered). */
private sealed interface Attempt {
    data class Finished(
        val outcome: DownloadOutcome,
    ) : Attempt

    data class Dropped(
        val connected: Boolean,
    ) : Attempt
}

/** One pass over the sources of a file: how it ended, and whether it ended only because the connection was missing or broke. */
private class Round(
    val outcome: DownloadOutcome,
    val lostConnection: Boolean,
)

/**
 * Downloads one book file into the books folder, the same folder and naming the backup restore
 * uses (the book's id and the format's ending). The file is written under a temporary name and
 * only renamed into place once it is complete and looks like what was asked for, so a stopped,
 * failed or cancelled download never leaves a half file or a record behind, and a web page sent
 * instead of a book is never kept as one. One download is one call; the caller decides how many
 * run at once. When the connection fails or breaks, the download waits five seconds and tries again, up to five times in all:
 * a broken transfer carries on from the bytes already received when the server supports byte ranges and otherwise starts that
 * file again. Only when the retries run out is the reader told the connection is down. A file is only fetched from an
 * address that [allowHost] accepts, the first one and
 * every redirect, by default Project Gutenberg and its mirrors over https.
 */
class BookDownloader internal constructor(
    private val client: OkHttpClient,
    private val storage: BookStorage,
    private val downloadedBookDao: DownloadedBookDao,
    private val timeProvider: TimeProvider,
    private val maxBytes: Long,
    private val allowHost: (HttpUrl) -> Boolean = DownloadSources::isAllowed,
    private val retryPauseMillis: Long = 0L,
    private val maxRetries: Int = MAX_RETRIES,
) {
    @Inject
    constructor(
        @DownloadClient client: OkHttpClient,
        storage: BookStorage,
        downloadedBookDao: DownloadedBookDao,
        timeProvider: TimeProvider,
    ) : this(
        client,
        storage,
        downloadedBookDao,
        timeProvider,
        MAX_FILE_BYTES,
        retryPauseMillis = RETRY_PAUSE_MILLIS,
    )

    /** Redirects are followed here, one by one, so that each address can be checked before it is asked for. */
    private val noRedirectClient = client.newBuilder().followRedirects(false).followSslRedirects(false).build()

    /** Downloads [url] as [format] of book [bookId], replacing any earlier copy of that format. */
    suspend fun download(
        bookId: Long,
        format: BookFileFormat,
        url: String,
        onProgress: (DownloadProgress) -> Unit = {},
    ): DownloadOutcome = downloadFirstThatWorks(bookId, format, listOf(url), onProgress)

    /**
     * The response for [request], following redirects one at a time. A redirect to an address that is
     * not allowed is never asked for. The client never follows a redirect from https to plain http, and
     * Project Gutenberg redirects some files that way, so such a redirect is followed to the same
     * address over https instead. A download never travels over plain http.
     */
    private suspend fun openFollowingRedirects(request: Request): Response {
        var current = request
        var hops = 0
        while (true) {
            val response = noRedirectClient.newCall(current).await()
            if (!response.isRedirect || hops >= MAX_REDIRECTS) return response
            val location = response.header("Location")?.let { response.request.url.resolve(it) } ?: return response
            response.close()
            val target =
                if (current.url.isHttps && !location.isHttps) location.newBuilder().scheme("https").build() else location
            if (!allowHost(target)) throw RedirectNotAllowedException(target.host)
            current = current.newBuilder().url(target).build()
            hops++
        }
    }

    private class RedirectNotAllowedException(val host: String) : Exception("Redirect to $host is not allowed")

    private fun logFailure(
        what: String,
        error: Throwable?,
    ) {
        // Logging must never break a download, and plain unit tests have no Android log.
        runCatching { Log.w("BookDownloader", what, error) }
    }

    /**
     * Tries [urls] in the order given until one delivers the book, and gives that outcome. The order
     * never changes. A site that cannot be reached is not tried again for the rest of the list, and a
     * file that is too large, or a phone with no room, stops the search, because another source would
     * fail the same way. When none works the outcome is [DownloadError.OFFLINE], or
     * [DownloadError.NOT_FOUND] when every source lacked the file. With a single address, its own error is reported.
     * When every source fails for lack of a connection the whole list is tried again after a pause, and a transfer that
     * breaks is carried on, both within one budget of retries, so the reader hears that the connection is down only after
     * the last one.
     */
    suspend fun downloadFirstThatWorks(
        bookId: Long,
        format: BookFileFormat,
        urls: List<String>,
        onProgress: (DownloadProgress) -> Unit = {},
    ): DownloadOutcome =
        withContext(Dispatchers.IO) {
            val partial = File(storage.directory, "$bookId.${format.extension}.part")
            try {
                retryingWhileOffline(bookId, format, urls, partial, onProgress)
            } finally {
                partial.delete()
            }
        }

    private suspend fun retryingWhileOffline(
        bookId: Long,
        format: BookFileFormat,
        urls: List<String>,
        partial: File,
        onProgress: (DownloadProgress) -> Unit,
    ): DownloadOutcome {
        val budget = RetryBudget(maxRetries)
        while (true) {
            val round = tryEach(bookId, format, urls, partial, budget, onProgress)
            if (!round.lostConnection || budget.left <= 0) return round.outcome
            budget.left--
            logFailure("No connection; trying again in ${retryPauseMillis / MILLIS_PER_SECOND} seconds", null)
            delay(retryPauseMillis)
        }
    }

    /** One pass over [urls]. [Round.lostConnection] is true when no source could be reached or kept its connection. */
    private suspend fun tryEach(
        bookId: Long,
        format: BookFileFormat,
        urls: List<String>,
        partial: File,
        budget: RetryBudget,
        onProgress: (DownloadProgress) -> Unit,
    ): Round {
        if (urls.size == 1) {
            val outcome = fromOneSource(bookId, format, urls.first(), partial, budget, onProgress)
            return Round(outcome, lostConnection = outcome == DownloadOutcome.Failed(DownloadError.OFFLINE))
        }
        val unreachable = mutableSetOf<String>()
        var everySourceLackedIt = urls.isNotEmpty()
        var everySourceLostConnection = urls.isNotEmpty()
        for (url in urls) {
            val site = siteOf(url)
            if (site != null && site in unreachable) continue
            when (val outcome = fromOneSource(bookId, format, url, partial, budget, onProgress)) {
                is DownloadOutcome.Done -> return Round(outcome, lostConnection = false)
                is DownloadOutcome.Failed -> {
                    val error = outcome.error
                    if (error == DownloadError.TOO_LARGE || error == DownloadError.STORAGE) return Round(outcome, lostConnection = false)
                    if (error == DownloadError.OFFLINE && site != null) unreachable.add(site)
                    if (error != DownloadError.NOT_FOUND) everySourceLackedIt = false
                    if (error != DownloadError.OFFLINE) everySourceLostConnection = false
                }
            }
        }
        val outcome = DownloadOutcome.Failed(if (everySourceLackedIt) DownloadError.NOT_FOUND else DownloadError.OFFLINE)
        return Round(outcome, lostConnection = everySourceLostConnection)
    }

    /** Downloads from one address. A transfer that breaks after the server answered is carried on while [budget] lasts. */
    private suspend fun fromOneSource(
        bookId: Long,
        format: BookFileFormat,
        url: String,
        partial: File,
        budget: RetryBudget,
        onProgress: (DownloadProgress) -> Unit,
    ): DownloadOutcome {
        val request =
            try {
                Request.Builder().url(url).build()
            } catch (e: IllegalArgumentException) {
                return DownloadOutcome.Failed(DownloadError.NOT_FOUND)
            }
        if (!allowHost(request.url)) {
            logFailure("Refused a download from ${request.url.host}: not an allowed address", null)
            return DownloadOutcome.Failed(DownloadError.NOT_FOUND)
        }
        // Whatever is left from another address or an earlier run is never carried on.
        partial.delete()
        while (true) {
            val resumeFrom = if (partial.isFile) partial.length() else 0L
            when (val attempt = attempt(request, bookId, format, partial, resumeFrom, onProgress)) {
                is Attempt.Finished -> return attempt.outcome
                is Attempt.Dropped -> {
                    if (!attempt.connected || budget.left <= 0) return DownloadOutcome.Failed(DownloadError.OFFLINE)
                    budget.left--
                    logFailure("The transfer broke; carrying on in ${retryPauseMillis / MILLIS_PER_SECOND} seconds", null)
                    delay(retryPauseMillis)
                }
            }
        }
    }

    /** Asks for the file, from byte [resumeFrom] when part of it is already in [partial], and stores what arrives. */
    private suspend fun attempt(
        request: Request,
        bookId: Long,
        format: BookFileFormat,
        partial: File,
        resumeFrom: Long,
        onProgress: (DownloadProgress) -> Unit,
    ): Attempt {
        val asked = if (resumeFrom > 0) request.newBuilder().header("Range", "bytes=$resumeFrom-").build() else request
        val response =
            try {
                openFollowingRedirects(asked)
            } catch (e: RedirectNotAllowedException) {
                logFailure("Refused a redirect to ${e.host}: not an allowed address", null)
                return Attempt.Finished(DownloadOutcome.Failed(DownloadError.NOT_FOUND))
            } catch (e: IOException) {
                logFailure("Could not reach ${request.url}", e)
                return Attempt.Dropped(connected = false)
            }
        return response.use { fetch(it, bookId, format, partial, resumeFrom, onProgress) }
    }

    private fun siteOf(url: String): String? = url.toHttpUrlOrNull()?.let { "${it.host}:${it.port}" }

    /** Deletes the downloaded file of [format] for book [bookId] and its record. Does nothing when there is none. */
    suspend fun delete(
        bookId: Long,
        format: BookFileFormat,
    ) {
        withContext(Dispatchers.IO) {
            val row =
                downloadedBookDao.getFormatsForBook(bookId).firstOrNull { it.format.equals(format.catalogName, ignoreCase = true) }
                    ?: return@withContext
            val file = File(row.localFilePath)
            // Only a file inside the books folder is ever deleted, whatever a record or a backup says.
            if (isInsideStorage(file)) file.delete()
            downloadedBookDao.delete(bookId, row.format)
        }
    }

    @SuppressLint("UsableSpace") // a plain free-space check is enough: the margin covers cache the system could clear
    private suspend fun fetch(
        response: Response,
        bookId: Long,
        format: BookFileFormat,
        partial: File,
        resumeFrom: Long,
        onProgress: (DownloadProgress) -> Unit,
    ): Attempt {
        if (resumeFrom > 0 && response.code == HTTP_RANGE_NOT_SATISFIABLE) {
            // What was kept does not fit the file any more: start that file again.
            partial.delete()
            return Attempt.Dropped(connected = true)
        }
        if (!response.isSuccessful) {
            logFailure("Status ${response.code} from ${response.request.url}", null)
            return failed(errorForStatus(response.code))
        }
        val body = response.body ?: return failed(DownloadError.SERVER)
        // A server that answers 206 from the byte asked for lets the download carry on; one that sends the whole file again
        // (status 200) is simply read from the start.
        val partialContent = resumeFrom > 0 && response.code == HTTP_PARTIAL_CONTENT
        if (partialContent && rangeStart(response.header("Content-Range")) != resumeFrom) {
            partial.delete()
            return Attempt.Dropped(connected = true)
        }
        val startAt = if (partialContent) resumeFrom else 0L
        val announced = body.contentLength().takeIf { it >= 0 }
        val total = announced?.let { it + startAt }
        if (total != null && total > maxBytes) return failed(DownloadError.TOO_LARGE)
        if (format != BookFileFormat.TEXT && isWebPage(response.header("Content-Type"))) return failed(DownloadError.NOT_A_BOOK)
        val directory = storage.directory
        if (!directory.isDirectory && !directory.mkdirs()) return failed(DownloadError.STORAGE)
        if (announced != null && directory.usableSpace < announced + FREE_SPACE_MARGIN_BYTES) return failed(DownloadError.STORAGE)

        val target = File(directory, "$bookId.${format.extension}")
        copyBody(body, partial, startAt, total, onProgress)?.let { error ->
            return if (error == DownloadError.OFFLINE) Attempt.Dropped(connected = true) else failed(error)
        }
        if (!looksLike(format, partial)) return failed(DownloadError.NOT_A_BOOK)
        target.delete()
        if (!partial.renameTo(target)) return failed(DownloadError.STORAGE)
        try {
            downloadedBookDao.insert(
                DownloadedBook(
                    bookId = bookId,
                    localFilePath = target.path,
                    format = format.catalogName,
                    downloadedAt = timeProvider.nowMillis(),
                ),
            )
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        return Attempt.Finished(DownloadOutcome.Done(target))
    }

    private fun failed(error: DownloadError): Attempt = Attempt.Finished(DownloadOutcome.Failed(error))

    /** The first byte a "Content-Range: bytes 100-999/1000" header says the body starts at, or null when it says nothing usable. */
    private fun rangeStart(contentRange: String?): Long? =
        contentRange?.trim()?.removePrefix("bytes")?.trim()?.substringBefore('-')?.trim()?.toLongOrNull()

    /**
     * Writes the body to [partial], after the [startAt] bytes it already holds when [startAt] is above 0 and from scratch
     * otherwise. Returns why it stopped, [DownloadError.OFFLINE] when the connection broke, or null when all of it was written.
     */
    private suspend fun copyBody(
        body: ResponseBody,
        partial: File,
        startAt: Long,
        total: Long?,
        onProgress: (DownloadProgress) -> Unit,
    ): DownloadError? {
        val source = body.source()
        val buffer = ByteArray(BUFFER_BYTES)
        var written = startAt
        var lastReported = startAt
        onProgress(DownloadProgress(startAt, total))
        try {
            FileOutputStream(partial, startAt > 0).use { output ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read =
                        try {
                            source.read(buffer)
                        } catch (e: IOException) {
                            logFailure("The connection broke while downloading", e)
                            return DownloadError.OFFLINE
                        }
                    if (read < 0) break
                    written += read
                    if (written > maxBytes) return DownloadError.TOO_LARGE
                    output.write(buffer, 0, read)
                    if (written - lastReported >= PROGRESS_STEP_BYTES) {
                        lastReported = written
                        onProgress(DownloadProgress(written, total))
                    }
                }
            }
        } catch (e: IOException) {
            return DownloadError.STORAGE
        }
        if (total != null && written < total) {
            logFailure("The connection ended after $written of $total bytes", null)
            return DownloadError.OFFLINE
        }
        onProgress(DownloadProgress(written, total ?: written))
        return null
    }

    private fun isInsideStorage(file: File): Boolean = file.canonicalFile.parentFile == storage.directory.canonicalFile

    private fun errorForStatus(code: Int): DownloadError =
        when (code) {
            403, 429, 451 -> DownloadError.BLOCKED
            404, 410 -> DownloadError.NOT_FOUND
            else -> DownloadError.SERVER
        }

    private fun isWebPage(contentType: String?): Boolean {
        val type = contentType?.substringBefore(';')?.trim()?.lowercase()
        return type == "text/html" || type == "application/xhtml+xml"
    }

    /** Whether the start of the file is what a file of [format] starts with. */
    private fun looksLike(
        format: BookFileFormat,
        file: File,
    ): Boolean {
        val head = ByteArray(HEAD_BYTES)
        val count = file.inputStream().use { it.read(head) }
        if (count <= 0) return false
        return when (format) {
            BookFileFormat.EPUB ->
                count >= 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte() && head[2] == 3.toByte() && head[3] == 4.toByte()
            BookFileFormat.PDF -> count >= 4 && String(head, 0, 4, Charsets.US_ASCII) == "%PDF"
            BookFileFormat.TEXT -> !startsLikeMarkup(head, count)
        }
    }

    /** True when the first thing in the file, after a byte order mark and spaces, is a "<". */
    private fun startsLikeMarkup(
        head: ByteArray,
        count: Int,
    ): Boolean {
        var index = 0
        if (count >= 3 && head[0] == 0xEF.toByte() && head[1] == 0xBB.toByte() && head[2] == 0xBF.toByte()) index = 3
        while (index < count && head[index].toInt().toChar().isWhitespace()) index++
        return index < count && head[index] == '<'.code.toByte()
    }

    private suspend fun Call.await(): Response =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancel() }
            enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        if (continuation.isActive) continuation.resume(response) else response.close()
                    }
                },
            )
        }
}
