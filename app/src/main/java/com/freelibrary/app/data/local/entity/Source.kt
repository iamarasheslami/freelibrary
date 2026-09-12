package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a book source (e.g. Project Gutenberg). Designed so additional
 * sources can be added later without touching the [books][Book] table's schema.
 */
@Entity(tableName = "sources")
data class Source(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "attribution")
    val attribution: String,
)
