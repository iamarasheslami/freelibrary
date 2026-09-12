package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-created bookmark within a book. Local-only, never synced. [position]
 * uses the same deferred text encoding as [ReadingProgress.lastPosition],
 * since both describe "a place in the book" and should stay consistent once
 * that format is finalized in Phase 3.
 */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["bookId"])],
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "position")
    val position: String,
    @ColumnInfo(name = "note")
    val note: String?,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,
)
