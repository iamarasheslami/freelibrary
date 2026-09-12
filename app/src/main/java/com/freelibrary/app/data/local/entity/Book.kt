package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A book in the catalog, from any source. [sourceId] + [externalId] together
 * identify the book within its origin (e.g. Project Gutenberg's own numeric ID),
 * while [id] is this app's own stable internal identifier used everywhere else
 * (join tables, reading progress, downloads).
 */
@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(
            entity = Source::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["sourceId", "externalId"], unique = true),
    ],
)
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sourceId")
    val sourceId: Long,
    @ColumnInfo(name = "externalId")
    val externalId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "issuedDate")
    val issuedDate: String?,
    @ColumnInfo(name = "primaryLanguage")
    val primaryLanguage: String?,
    @ColumnInfo(name = "locc")
    val locc: String?,
)
