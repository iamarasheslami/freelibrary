package com.freelibrary.app.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The name every backup file declares in its manifest, so a foreign zip is recognised and refused. */
const val BACKUP_FORMAT = "freelibrary-backup"

/** The layout version of the backup file. A newer version than this app knows is refused, not guessed at. */
const val BACKUP_VERSION = 1

/** The names of the files inside a backup zip. Book files are named by number, never by anything a book or reader chose. */
object BackupEntries {
    const val MANIFEST = "manifest.json"
    const val DATA = "data.json"
    const val SETTINGS = "settings.json"
    const val BOOKS = "books.json"
    const val AVATAR = "profile/avatar.jpg"
    const val BOOK_FILE_PREFIX = "books/"
}

/** The parts of a backup the reader chooses between, on export and again on import. */
@Serializable
enum class BackupPart {
    /** Shelves, reading positions, bookmarks and highlights. */
    DATA,

    /** Theme, true black and Wi-Fi only sync. */
    SETTINGS,

    /** The downloaded book files. */
    BOOKS,
}

/** One JSON setup for every backup file: unknown keys are ignored so an older app can read a newer file's known parts. */
val BackupJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

/** A book named by where it comes from, not by this phone's internal id, which differs between phones. */
@Serializable
data class BookRef(
    val source: String,
    val externalId: String,
)

@Serializable
data class BackupManifest(
    val format: String = BACKUP_FORMAT,
    val version: Int = BACKUP_VERSION,
    val createdAt: Long,
    val parts: List<BackupPart>,
)

@Serializable
data class ShelfStateBackup(
    val book: BookRef,
    val state: String,
    val updatedAt: Long,
    val finishedAt: Long? = null,
)

@Serializable
data class ReadingProgressBackup(
    val book: BookRef,
    val lastPosition: String,
    val lastOpenedAt: Long,
)

@Serializable
data class BookmarkBackup(
    val book: BookRef,
    val position: String,
    val note: String? = null,
    val createdAt: Long,
)

@Serializable
data class HighlightBackup(
    val book: BookRef,
    val startPosition: String,
    val endPosition: String,
    val color: String? = null,
    val createdAt: Long,
)

/** The DATA part. */
@Serializable
data class DataBackup(
    val shelfStates: List<ShelfStateBackup> = emptyList(),
    val readingProgress: List<ReadingProgressBackup> = emptyList(),
    val bookmarks: List<BookmarkBackup> = emptyList(),
    val highlights: List<HighlightBackup> = emptyList(),
)

/** The SETTINGS part. */
@Serializable
data class SettingsBackup(
    val themeMode: String,
    val wifiOnlySync: Boolean,
    /** The reader's name, birthday and picture marker. Absent in backups made before profiles existed. */
    val profile: ProfileBackup? = null,
)

/** The profile inside the SETTINGS part. The picture itself follows as the [BackupEntries.AVATAR] zip entry. */
@Serializable
data class ProfileBackup(
    val name: String? = null,
    val birthdayUtcMillis: Long? = null,
    val hasPicture: Boolean = false,
)

/** One downloaded book file: [entry] is its name inside the zip. */
@Serializable
data class BookFileBackup(
    val book: BookRef,
    val format: String,
    val entry: String,
    val downloadedAt: Long,
)

/** The BOOKS part's list; the files themselves follow as separate zip entries. */
@Serializable
data class BooksBackup(
    val files: List<BookFileBackup> = emptyList(),
)
