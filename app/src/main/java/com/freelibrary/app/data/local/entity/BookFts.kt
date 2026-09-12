package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * A standalone full-text search index over title and author names only.
 * Not linked via Room's "external content" mode, since that only maps to a
 * single source entity's own columns — author names come from a many-to-many
 * join, so this table stores a denormalized, pre-combined copy of the
 * searchable text instead.
 *
 * [bookId] is mapped to the column name "rowid", which Room requires for an
 * FTS entity's primary key, while keeping the Kotlin property name readable.
 *
 * This table is NOT auto-synced by Room. Whenever a book or its author
 * credits change, the repository/import layer is responsible for
 * inserting/updating the corresponding row here. FTS4 is used rather than
 * FTS5, since Room 2.x does not support FTS5 (older Android SQLite builds
 * lack it).
 */
@Fts4
@Entity(tableName = "books_fts")
data class BookFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val bookId: Long,
    val title: String,
    val authorNames: String,
)
