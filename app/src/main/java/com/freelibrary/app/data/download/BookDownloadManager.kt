package com.freelibrary.app.data.download

import android.util.Log
import com.freelibrary.app.data.local.dao.BookFormatDao
import com.freelibrary.app.data.local.dao.DownloadedBookDao
import com.freelibrary.app.domain.model.BookFileFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BookDownloadManager"

/** A download of one book that is waiting, running or has failed; a finished one is no longer a transfer. */
sealed interface Transfer {
    val format: BookFileFormat

    data class Waiting(
        override val format: BookFileFormat,
    ) : Transfer

    data class Running(
        override val format: BookFileFormat,
        val progress: DownloadProgress,
    ) : Transfer

    data class Failed(
        override val format: BookFileFormat,
        val error: DownloadError,
    ) : Transfer
}

/** What a book page needs to know about the book's files. */
data class BookFiles(
    /** The formats the catalog offers for the book and the app can open, in offering order. */
    val available: List<BookFileFormat>,
    /** The formats already on the phone, in offering order. */
    val downloaded: List<BookFileFormat>,
    /** The download in progress or the last one that failed, if any. */
    val transfer: Transfer?,
) {
    /** True while a download of this book is waiting its turn or running. */
    val busy: Boolean get() = transfer is Transfer.Waiting || transfer is Transfer.Running

    /** The offered formats not on the phone yet, in offering order; the first is the default. */
    val toDownload: List<BookFileFormat> get() = available.filter { it !in downloaded }
}

/**
 * Runs the reader's downloads for the whole app, so a download goes on when its page is closed.
 * Downloads run one at a time, as Project Gutenberg asks of apps: others wait their turn. A book
 * with a download waiting or running ignores another request for itself. A failure is remembered,
 * with its reason, until the reader tries again or cancels. Nothing here survives the app being
 * closed; a download stopped that way leaves no half file and is simply started again.
 */
@Singleton
class BookDownloadManager
    @Inject
    constructor(
        private val downloader: BookDownloader,
        private val bookFormatDao: BookFormatDao,
        private val downloadedBookDao: DownloadedBookDao,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val oneAtATime = Mutex()
        private val transfers = MutableStateFlow<Map<Long, Transfer>>(emptyMap())
        private val jobs = ConcurrentHashMap<Long, Job>()

        /** The files of one book, kept live. */
        fun observe(bookId: Long): Flow<BookFiles> {
            val transfer = transfers.map { it[bookId] }.distinctUntilChanged()
            return combine(
                flow { emit(bookFormatDao.getFormatsForBook(bookId).map { it.formatType }) },
                downloadedBookDao.observeForBook(bookId),
                transfer,
            ) { catalog, downloadedRows, current ->
                BookFiles(
                    available = BookFileFormat.offered(catalog),
                    downloaded = BookFileFormat.offered(downloadedRows.map { it.format }),
                    transfer = current,
                )
            }
        }

        /** Starts downloading [format] of [bookId]. Ignored while a download of that book is waiting or running. */
        fun start(
            bookId: Long,
            format: BookFileFormat,
        ) {
            val job = scope.launch(start = CoroutineStart.LAZY) { run(bookId, format) }
            if (jobs.putIfAbsent(bookId, job) != null) {
                job.cancel()
                return
            }
            transfers.update { it + (bookId to Transfer.Waiting(format)) }
            job.invokeOnCompletion { jobs.remove(bookId, job) }
            job.start()
        }

        /** Stops the download of [bookId] if there is one, and forgets a failure. */
        fun cancel(bookId: Long) {
            jobs.remove(bookId)?.cancel()
            transfers.update { it - bookId }
        }

        /** Deletes the downloaded [format] of [bookId]. */
        suspend fun delete(
            bookId: Long,
            format: BookFileFormat,
        ) = downloader.delete(bookId, format)

        private suspend fun run(
            bookId: Long,
            format: BookFileFormat,
        ) {
            try {
                val url =
                    bookFormatDao.getFormatsForBook(bookId)
                        .firstOrNull { it.formatType.equals(format.catalogName, ignoreCase = true) }
                        ?.downloadUrl
                if (url == null) {
                    fail(bookId, format, DownloadError.NOT_FOUND)
                    return
                }
                oneAtATime.withLock {
                    transfers.update { it + (bookId to Transfer.Running(format, DownloadProgress(0L, null))) }
                    val outcome =
                        downloader.downloadFirstThatWorks(bookId, format, DownloadSources.candidates(url, format)) { progress ->
                            transfers.update { current ->
                                if (current[bookId] is Transfer.Running) {
                                    current + (bookId to Transfer.Running(format, progress))
                                } else {
                                    current
                                }
                            }
                        }
                    when (outcome) {
                        is DownloadOutcome.Done -> transfers.update { it - bookId }
                        is DownloadOutcome.Failed -> fail(bookId, format, outcome.error)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "The download stopped unexpectedly", e)
                fail(bookId, format, DownloadError.STORAGE)
            }
        }

        private fun fail(
            bookId: Long,
            format: BookFileFormat,
            error: DownloadError,
        ) {
            transfers.update { it + (bookId to Transfer.Failed(format, error)) }
        }
    }
