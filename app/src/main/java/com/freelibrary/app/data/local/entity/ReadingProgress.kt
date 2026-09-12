package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Local-only reading position for a single book. Never synced anywhere.
 * [lastPosition] is deliberately a plain string rather than a fixed numeric
 * type — its exact encoding (character offset, percentage, EPUB CFI, etc.)
 * is decided when the reader screen is built in Phase 3; storing it as text
 * now avoids locking in a format prematurely.
 */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReadingProgress(
    @PrimaryKey
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "lastPosition")
    val lastPosition: String,
    @ColumnInfo(name = "lastOpenedAt")
    val lastOpenedAt: Long,
)
