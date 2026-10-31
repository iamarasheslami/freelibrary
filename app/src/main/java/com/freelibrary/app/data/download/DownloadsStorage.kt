package com.freelibrary.app.data.download

import com.freelibrary.app.data.local.dao.DownloadedBookDao
import com.freelibrary.app.domain.model.BookFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

/**
 * The books downloaded to this phone, for the Storage page: what they are, how much room each takes,
 * and a way to remove one for good. Removing uses the same code as the Delete button of the book page.
 */
class DownloadsStorage
    @Inject
    constructor(
        private val downloadedBookDao: DownloadedBookDao,
        private val downloader: BookDownloader,
    ) {
        /** Every downloaded file, newest first, kept live. A record whose file is gone shows with a size of 0. */
        fun observe(): Flow<List<StoredDownload>> =
            downloadedBookDao
                .observeAllWithTitles()
                .map { rows ->
                    rows.mapNotNull { row ->
                        val format = BookFileFormat.fromCatalog(row.format)
                        format?.let { StoredDownload(row.bookId, row.title, it, File(row.localFilePath).length()) }
                    }
                }.flowOn(Dispatchers.IO)

        /** Deletes the file of [download] from the phone and forgets the record. */
        suspend fun delete(download: StoredDownload) = downloader.delete(download.bookId, download.format)
    }
