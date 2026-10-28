package com.freelibrary.app.data.backup

import com.freelibrary.app.data.local.dao.BackupDao
import com.freelibrary.app.data.local.dao.BookShelfStateDao
import com.freelibrary.app.data.local.dao.BookmarkDao
import com.freelibrary.app.data.local.dao.DownloadedBookDao
import com.freelibrary.app.data.local.dao.HighlightDao
import com.freelibrary.app.data.local.dao.ReadingProgressDao
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookmark
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.data.local.entity.Highlight
import com.freelibrary.app.data.local.entity.ReadingProgress
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.repository.SettingsRepository
import com.freelibrary.app.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/** The file is not a backup this app can read: another kind of file, damaged, or made by a newer app. */
class BackupFormatException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** What a backup file holds, read from its manifest alone. */
data class BackupInfo(
    val createdAt: Long,
    val parts: List<BackupPart>,
)

/** What an import did, for the message the reader sees. */
data class ImportSummary(
    /** Shelf states, reading positions, bookmarks and highlights written. */
    val restoredItems: Int,
    /** Items not written: their book is not in this phone's catalog, or this phone's own copy is as new or newer. */
    val skippedItems: Int,
    val settingsRestored: Boolean,
    val bookFilesRestored: Int,
    /** Book files not restored: the book is not in this phone's catalog, or the phone already has that file. */
    val bookFilesSkipped: Int,
    /** True when a name, birthday or picture from the backup was restored. */
    val profileRestored: Boolean = false,
)

private const val MAX_JSON_BYTES = 32 * 1024 * 1024
private const val COPY_BUFFER_BYTES = 8 * 1024
private const val MAX_EXTENSION_LENGTH = 10

/** The largest book file a backup may restore; the same limit as a download, so a crafted backup cannot fill the phone. */
private const val MAX_BOOK_FILE_BYTES = 300L * 1024 * 1024

/**
 * Reads a backup zip and merges it into this phone, never replacing something newer. The zip is
 * read once from start to end, so the manifest and the lists come before the book files they
 * describe. Book files are written under names made here (the book's id and a cleaned-up format),
 * never under a name from the zip, so a crafted backup cannot write outside the books folder.
 * Every item is written on its own, so an import that stops half way leaves a valid state and
 * running it again finishes the job without duplicating anything.
 */
@Singleton
class BackupImporter
    @Inject
    constructor(
        private val backupDao: BackupDao,
        private val bookShelfStateDao: BookShelfStateDao,
        private val readingProgressDao: ReadingProgressDao,
        private val bookmarkDao: BookmarkDao,
        private val highlightDao: HighlightDao,
        private val downloadedBookDao: DownloadedBookDao,
        private val settingsRepository: SettingsRepository,
        private val bookStorage: BookStorage,
        private val profileBackupStore: ProfileBackupStore,
    ) {
        /** Reads only the manifest, so the reader can be asked which of the file's parts to restore. */
        suspend fun inspect(input: InputStream): BackupInfo =
            withContext(Dispatchers.IO) {
                try {
                    ZipInputStream(BufferedInputStream(input)).use { zip ->
                        val manifest = readManifest(zip)
                        BackupInfo(manifest.createdAt, manifest.parts)
                    }
                } catch (e: ZipException) {
                    throw BackupFormatException("The file is damaged", e)
                }
            }

        suspend fun import(
            input: InputStream,
            parts: Set<BackupPart>,
        ): ImportSummary {
            require(parts.isNotEmpty()) { "Choose at least one part to restore" }
            return withContext(Dispatchers.IO) {
                try {
                    ZipInputStream(BufferedInputStream(input)).use { zip ->
                        val wanted = parts intersect readManifest(zip).parts.toSet()
                        val tally = Tally()
                        var bookFiles: Map<String, BookFileBackup> = emptyMap()
                        while (true) {
                            ensureActive()
                            val entry = zip.nextEntry ?: break
                            when {
                                entry.name == BackupEntries.DATA && BackupPart.DATA in wanted ->
                                    restoreData(decode(DataBackup.serializer(), readJson(zip)), tally)
                                entry.name == BackupEntries.SETTINGS && BackupPart.SETTINGS in wanted ->
                                    restoreSettings(decode(SettingsBackup.serializer(), readJson(zip)), tally)
                                entry.name == BackupEntries.AVATAR && BackupPart.SETTINGS in wanted ->
                                    if (profileBackupStore.restorePicture(zip)) tally.profile = true
                                entry.name == BackupEntries.BOOKS && BackupPart.BOOKS in wanted ->
                                    bookFiles = decode(BooksBackup.serializer(), readJson(zip)).files.associateBy { it.entry }
                                entry.name.startsWith(BackupEntries.BOOK_FILE_PREFIX) && BackupPart.BOOKS in wanted ->
                                    bookFiles[entry.name]?.let { restoreBookFile(it, zip, tally) }
                            }
                        }
                        tally.toSummary()
                    }
                } catch (e: ZipException) {
                    throw BackupFormatException("The file is damaged", e)
                }
            }
        }

        private class Tally {
            var restored = 0
            var skipped = 0
            var settings = false
            var profile = false
            var filesRestored = 0
            var filesSkipped = 0

            fun toSummary() = ImportSummary(restored, skipped, settings, filesRestored, filesSkipped, profile)
        }

        private fun readManifest(zip: ZipInputStream): BackupManifest {
            val first = zip.nextEntry
            if (first == null || first.name != BackupEntries.MANIFEST) throw BackupFormatException("This is not a FreeLibrary backup")
            val manifest = decode(BackupManifest.serializer(), readJson(zip))
            if (manifest.format != BACKUP_FORMAT) throw BackupFormatException("This is not a FreeLibrary backup")
            if (manifest.version > BACKUP_VERSION) throw BackupFormatException("This backup was made by a newer version of the app")
            return manifest
        }

        private fun readJson(zip: ZipInputStream): String {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            var total = 0
            while (true) {
                val read = zip.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_JSON_BYTES) throw BackupFormatException("The file is not a valid backup")
                out.write(buffer, 0, read)
            }
            return out.toString(Charsets.UTF_8.name())
        }

        private fun <T> decode(
            serializer: KSerializer<T>,
            text: String,
        ): T =
            try {
                BackupJson.decodeFromString(serializer, text)
            } catch (e: SerializationException) {
                throw BackupFormatException("The file is damaged", e)
            }

        private suspend fun bookId(book: BookRef): Long? = backupDao.bookId(book.source, book.externalId)

        private suspend fun restoreData(
            data: DataBackup,
            tally: Tally,
        ) {
            data.shelfStates.forEach { item ->
                val id = bookId(item.book)
                val state = ShelfState.entries.firstOrNull { it.name == item.state }
                val existing = id?.let { bookShelfStateDao.getForBook(it) }
                if (id == null || state == null || (existing != null && item.updatedAt <= existing.updatedAt)) {
                    tally.skipped++
                } else {
                    bookShelfStateDao.upsert(BookShelfState(id, state, item.updatedAt, item.finishedAt))
                    tally.restored++
                }
            }
            data.readingProgress.forEach { item ->
                val id = bookId(item.book)
                val existing = id?.let { readingProgressDao.getForBook(it) }
                if (id == null || (existing != null && item.lastOpenedAt <= existing.lastOpenedAt)) {
                    tally.skipped++
                } else {
                    readingProgressDao.upsert(ReadingProgress(id, item.lastPosition, item.lastOpenedAt))
                    tally.restored++
                }
            }
            data.bookmarks.forEach { item ->
                val id = bookId(item.book)
                val duplicate =
                    id?.let {
                        bookmarkDao.getForBook(it).any {
                                b ->
                            b.position == item.position && b.createdAt == item.createdAt
                        }
                    }
                if (id == null || duplicate == true) {
                    tally.skipped++
                } else {
                    bookmarkDao.insert(Bookmark(bookId = id, position = item.position, note = item.note, createdAt = item.createdAt))
                    tally.restored++
                }
            }
            data.highlights.forEach { item ->
                val id = bookId(item.book)
                val duplicate =
                    id?.let {
                        highlightDao.getForBook(it).any { h ->
                            h.startPosition == item.startPosition && h.endPosition == item.endPosition && h.createdAt == item.createdAt
                        }
                    }
                if (id == null || duplicate == true) {
                    tally.skipped++
                } else {
                    highlightDao.insert(
                        Highlight(
                            bookId = id,
                            startPosition = item.startPosition,
                            endPosition = item.endPosition,
                            color = item.color,
                            createdAt = item.createdAt,
                        ),
                    )
                    tally.restored++
                }
            }
        }

        private suspend fun restoreSettings(
            settings: SettingsBackup,
            tally: Tally,
        ) {
            ThemeMode.entries.firstOrNull { it.name == settings.themeMode }?.let { settingsRepository.setThemeMode(it) }
            settingsRepository.setWifiOnlySync(settings.wifiOnlySync)
            settings.profile?.let { profile ->
                profileBackupStore.restoreDetails(profile)
                if (profile.name != null || profile.birthdayUtcMillis != null) tally.profile = true
            }
            tally.settings = true
        }

        private suspend fun restoreBookFile(
            item: BookFileBackup,
            zip: ZipInputStream,
            tally: Tally,
        ) {
            val id = bookId(item.book)
            val existing = id?.let { downloadedBookDao.getFormatsForBook(it).firstOrNull { d -> d.format == item.format } }
            if (id == null || (existing != null && File(existing.localFilePath).isFile)) {
                tally.filesSkipped++
                return
            }
            val directory = bookStorage.directory
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("The books folder could not be created")
            val name = "$id.${safeExtension(item.format)}"
            val target = File(directory, name)
            val partial = File(directory, "$name.part")
            try {
                val complete = partial.outputStream().use { copyAtMost(zip, it, MAX_BOOK_FILE_BYTES) }
                if (!complete) {
                    tally.filesSkipped++
                    return
                }
                target.delete()
                if (!partial.renameTo(target)) throw IOException("The book file could not be saved")
            } finally {
                partial.delete()
            }
            downloadedBookDao.insert(
                DownloadedBook(bookId = id, localFilePath = target.path, format = item.format, downloadedAt = item.downloadedAt),
            )
            tally.filesRestored++
        }

        /** The format cleaned down to lowercase letters and digits, so it can never carry a path. */
        private fun safeExtension(format: String): String =
            format.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.take(MAX_EXTENSION_LENGTH).ifEmpty { "bin" }
    }

/**
 * Copies [input] to [output] but gives up once more than [limit] bytes have been read, so a file of any
 * size can be refused without being written out. Returns false in that case; the output then holds
 * only the first [limit] bytes at most and must be discarded.
 */
internal fun copyAtMost(
    input: InputStream,
    output: OutputStream,
    limit: Long,
): Boolean {
    val buffer = ByteArray(COPY_BUFFER_BYTES)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return true
        total += read
        if (total > limit) return false
        output.write(buffer, 0, read)
    }
}
