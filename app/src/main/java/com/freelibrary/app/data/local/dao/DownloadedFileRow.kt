package com.freelibrary.app.data.local.dao

/** One downloaded file with the title of its book, for the Storage page. */
data class DownloadedFileRow(
    val bookId: Long,
    val title: String,
    val format: String,
    val localFilePath: String,
)
