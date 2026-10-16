package com.freelibrary.app.data.local.dao

/** One reader row for a backup, with its book named by source and external id instead of the internal id. */
data class ShelfStateRow(
    val source: String,
    val externalId: String,
    val state: String,
    val updatedAt: Long,
    val finishedAt: Long?,
)

data class ReadingProgressRow(
    val source: String,
    val externalId: String,
    val lastPosition: String,
    val lastOpenedAt: Long,
)

data class BookmarkRow(
    val source: String,
    val externalId: String,
    val position: String,
    val note: String?,
    val createdAt: Long,
)

data class HighlightRow(
    val source: String,
    val externalId: String,
    val startPosition: String,
    val endPosition: String,
    val color: String?,
    val createdAt: Long,
)

data class DownloadRow(
    val source: String,
    val externalId: String,
    val localFilePath: String,
    val format: String,
    val downloadedAt: Long,
)
