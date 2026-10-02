package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * A book's catalog summary, kept in its own table rather than a column on
 * [Book] so that everyday book queries and lists never load this long text.
 * At most one row per book; absent when the catalog has no summary for it.
 *
 * Some summaries are machine-generated and say so in their own text, which
 * is kept as-is so users are not misled about their origin.
 */
@Entity(
    tableName = "book_summaries",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BookSummary(
    @PrimaryKey
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "summary")
    val summary: String,
)
