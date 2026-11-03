package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A calendar day on which the reader read, kept only on this phone. [day] counts whole days since 1 January 1970 on the
 * phone's own calendar, so a day is one row however many times the reader opened a book on it.
 */
@Entity(tableName = "reading_days")
data class ReadingDay(
    @PrimaryKey
    @ColumnInfo(name = "day")
    val day: Long,
)
