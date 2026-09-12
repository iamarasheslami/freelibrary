package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An available download format for a book. [downloadUrl] is the fully resolved
 * URL, computed once when the catalog is built — the app never needs to know
 * which source a book came from or how that source structures its URLs.
 */
@Entity(
    tableName = "book_formats",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "formatType"], unique = true),
    ],
)
data class BookFormat(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "formatType")
    val formatType: String,
    @ColumnInfo(name = "downloadUrl")
    val downloadUrl: String,
)
