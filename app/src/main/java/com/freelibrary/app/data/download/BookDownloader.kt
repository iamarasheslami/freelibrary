package com.freelibrary.app.data.download

import android.util.Log
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.backup.BookStorage
import com.freelibrary.app.data.local.dao.DownloadedBookDao
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.domain.model.BookFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val MAX_FILE_BYTES = 300L * 1024 * 1024
private const val FREE_SPACE_MARGIN_BYTES = 20L * 1024 * 1024
private const val BUFFER_BYTES = 16 * 1024
private const val PROGRESS_STEP_BYTES = 64L * 1024
private const val HEAD_BYTES = 512

/**
 * Downloads one book file into the books folder, the same folder and naming the backup restore
 * uses (the book's id and the format's ending). The file is written under a temporary name and
 * only renamed into place once it is complete and looks like what was asked for, so a stopped,
 * failed or cancelled download never leaves a half file or a record behind, and a web page sent
 * instead of a book is never kept as one. One download is one call; the caller decides how many
 * run at once.
 */
class BookDownloader internal constructor(
    private val client: OkHttpClient,
    private val storage: BookStorage,
    private val downloadedBookDao: DownloadedBookDao,
    private val timeProvider: TimeProvider,
    private val maxBytes: Long,
) {
    @Inject
    constructor(
        @DownloadClient client: OkHttpClient,
        storage: BookStorage,
        downloadedBookDao: DownloadedBookDao,
        timeProvider: TimeProvider,
    ) : this(client, storage, downloadedBookDao, timeProvider, MAX_FILE_BYTES)

    /** Downloads [url] as [format] of book [bookId], replacing any earlier copy of that format. */
    suspend fun download(
        bookId: Long,
        format: BookFileFormat,
        url: String,
        onProgress: (DownloadProgress) -> Unit = {},
    ): DownloadOutcome =
        withContext(Dispatchers.IO) {
            val request =
                try {
                    Request.Builder().url(url).build()
                } catch (e: IllegalArgumentException) {
                    return@withContext DownloadOutcome.Failed(DownloadError.NOT_FOUND)
                }
            val response =
                try {
                    openFollowingInsecureRedirects(request)
                } catch (e: IOException) {
                    logFailure("Could not reach $url", e)
                    return@withContext DownloadOutcome.Failed(DownloadError.OFFLINE)
                }
            response.use { fetch(it, bookId, format, onProgress) }
        }

    /**
     * The response for [request]. The client never follows a redirect from https to plain http, and
     * Project Gutenberg redirects some files that way, so such a redirect is followed to the same
     * address over https instead. A download never travels over plain http.
     */
    private suspend fun openFollowingInsecureRedirects(request: Request): Response {
        val maxHops = 5
        var response = client.newCall(request).await()
        var hops = 0
        while (response.isRedirect && hops < maxHops) {
            val target = response.header("Location")?.let { response.request.url.resolve(it) } ?: break
            if (target.isHttps) break
            response.close()
            val secure = request.newBuilder().url(target.newBuilder().scheme("https").build()).build()
            response = client.newCall(secure).await()
            hops++
        }
        return response
    }

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
     */
    suspend fun downloadFirstThatWorks(
        bookId: Long,
        format: BookFileFormat,
        urls: List<String>,
        onProgress: (DownloadProgress) -> Unit = {},
    ): DownloadOutcome {
        if (urls.size == 1) return download(bookId, format, urls.first(), onProgress)
        val unreachable = mutableSetOf<String>()
        var everySourceLackedIt = urls.isNotEmpty()
        for (url in urls) {
            val site = siteOf(url)
            if (site != null && site in unreachable) continue
            when (val outcome = download(bookId, format, url, onProgress)) {
                is DownloadOutcome.Done -> return outcome
                is DownloadOutcome.Failed -> {
                    val error = outcome.error
                    if (error == DownloadError.TOO_LARGE || error == DownloadError.STORAGE) return outcome
                    if (error == DownloadError.OFFLINE && site != null) unreachable.add(site)
                    if (error != DownloadError.NOT_FOUND) everySourceLackedIt = false
                }
            }
        }
        return DownloadOutcome.Failed(if (everySourceLackedIt) DownloadError.NOT_FOUND else DownloadError.OFFLINE)
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

    private suspend fun fetch(
        response: Response,
        bookId: Long,
        format: BookFileFormat,
        onProgress: (DownloadProgress) -> Unit,
    ): DownloadOutcome {
        if (!response.isSuccessful) {
            logFailure("Status ${response.code} from ${response.request.url}", null)
            return DownloadOutcome.Failed(errorForStatus(response.code))
        }
        val body = response.body ?: return DownloadOutcome.Failed(DownloadError.SERVER)
        val total = body.contentLength().takeIf { it >= 0 }
        if (total != null && total > maxBytes) return DownloadOutcome.Failed(DownloadError.TOO_LARGE)
        if (format != BookFileFormat.TEXT && isWebPage(response.header("Content-Type"))) {
            return DownloadOutcome.Failed(DownloadError.NOT_A_BOOK)
        }
        val directory = storage.directory
        if (!directory.isDirectory && !directory.mkdirs()) return DownloadOutcome.Failed(DownloadError.STORAGE)
        if (total != null && directory.usableSpace < total + FREE_SPACE_MARGIN_BYTES) return DownloadOutcome.Failed(DownloadError.STORAGE)

        val target = File(directory, "$bookId.${format.extension}")
        val partial = File(directory, "${target.name}.part")
        try {
            copyBody(body, partial, total, onProgress)?.let { return DownloadOutcome.Failed(it) }
            if (!looksLike(format, partial)) return DownloadOutcome.Failed(DownloadError.NOT_A_BOOK)
            target.delete()
            if (!partial.renameTo(target)) return DownloadOutcome.Failed(DownloadError.STORAGE)
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
            return DownloadOutcome.Done(target)
        } finally {
            partial.delete()
        }
    }

    /** Writes the body to [partial]. Returns why it stopped, or null when all of it was written. */
    private suspend fun copyBody(
        body: ResponseBody,
        partial: File,
        total: Long?,
        onProgress: (DownloadProgress) -> Unit,
    ): DownloadError? {
        val source = body.source()
        val buffer = ByteArray(BUFFER_BYTES)
        var written = 0L
        var lastReported = 0L
        onProgress(DownloadProgress(0L, total))
        try {
            partial.outputStream().use { output ->
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
