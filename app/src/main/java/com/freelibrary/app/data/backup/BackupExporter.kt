package com.freelibrary.app.data.backup

import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.dao.BackupDao
import com.freelibrary.app.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** What an export wrote, for the message the reader sees. */
data class ExportSummary(
    val parts: Set<BackupPart>,
    /** Shelf states, reading positions, bookmarks and highlights written. */
    val dataItems: Int,
    val bookFiles: Int,
    /** Downloads the database lists but whose file is no longer on the phone; they are left out. */
    val missingBookFiles: Int,
)

/**
 * Writes a backup zip: the manifest first, then each part the reader chose. Books are named by
 * source and external id, and every zip entry has a fixed name or a number, never a name taken
 * from the reader's data or the book files' paths. The zip is written straight to [output] one
 * entry at a time, so a large library never has to fit in memory.
 */
@Singleton
class BackupExporter
    @Inject
    constructor(
        private val backupDao: BackupDao,
        private val settingsRepository: SettingsRepository,
        private val timeProvider: TimeProvider,
        private val profileBackupStore: ProfileBackupStore,
    ) {
        suspend fun export(
            parts: Set<BackupPart>,
            output: OutputStream,
        ): ExportSummary {
            require(parts.isNotEmpty()) { "Choose at least one part to export" }
            return withContext(Dispatchers.IO) {
                ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                    val manifest = BackupManifest(createdAt = timeProvider.nowMillis(), parts = BackupPart.entries.filter { it in parts })
                    zip.putText(BackupEntries.MANIFEST, BackupJson.encodeToString(BackupManifest.serializer(), manifest))

                    var dataItems = 0
                    if (BackupPart.DATA in parts) {
                        val data = readData()
                        dataItems = data.shelfStates.size + data.readingProgress.size + data.bookmarks.size + data.highlights.size
                        zip.putText(BackupEntries.DATA, BackupJson.encodeToString(DataBackup.serializer(), data))
                    }
                    if (BackupPart.SETTINGS in parts) {
                        val profile = profileBackupStore.read()
                        zip.putText(BackupEntries.SETTINGS, BackupJson.encodeToString(SettingsBackup.serializer(), readSettings(profile)))
                        if (profile?.hasPicture == true) {
                            profileBackupStore.pictureFile()?.let { picture ->
                                zip.putNextEntry(ZipEntry(BackupEntries.AVATAR))
                                picture.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }

                    var bookFiles = 0
                    var missing = 0
                    if (BackupPart.BOOKS in parts) {
                        val downloads = backupDao.downloads()
                        val present = downloads.filter { File(it.localFilePath).isFile }
                        missing = downloads.size - present.size
                        val entries =
                            present.mapIndexed { index, row ->
                                BookFileBackup(
                                    book = BookRef(row.source, row.externalId),
                                    format = row.format,
                                    entry = "${BackupEntries.BOOK_FILE_PREFIX}${index + 1}",
                                    downloadedAt = row.downloadedAt,
                                )
                            }
                        zip.putText(BackupEntries.BOOKS, BackupJson.encodeToString(BooksBackup.serializer(), BooksBackup(entries)))
                        // Ebooks are already compressed; the fastest level keeps a weak phone from working hard for nothing.
                        zip.setLevel(Deflater.BEST_SPEED)
                        present.forEachIndexed { index, row ->
                            ensureActive()
                            zip.putNextEntry(ZipEntry(entries[index].entry))
                            File(row.localFilePath).inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                            bookFiles++
                        }
                    }
                    ExportSummary(parts = parts, dataItems = dataItems, bookFiles = bookFiles, missingBookFiles = missing)
                }
            }
        }

        private suspend fun readData(): DataBackup =
            DataBackup(
                shelfStates =
                    backupDao.shelfStates().map {
                        ShelfStateBackup(
                            BookRef(it.source, it.externalId),
                            it.state,
                            it.updatedAt,
                            it.finishedAt,
                        )
                    },
                readingProgress =
                    backupDao.readingProgress().map {
                        ReadingProgressBackup(BookRef(it.source, it.externalId), it.lastPosition, it.lastOpenedAt)
                    },
                bookmarks =
                    backupDao.bookmarks().map {
                        BookmarkBackup(
                            BookRef(it.source, it.externalId),
                            it.position,
                            it.note,
                            it.createdAt,
                        )
                    },
                highlights =
                    backupDao.highlights().map {
                        HighlightBackup(BookRef(it.source, it.externalId), it.startPosition, it.endPosition, it.color, it.createdAt)
                    },
            )

        private suspend fun readSettings(profile: ProfileBackup?): SettingsBackup =
            SettingsBackup(
                themeMode = settingsRepository.themeMode.first().name,
                wifiOnlySync = settingsRepository.wifiOnlySync.first(),
                profile = profile,
            )

        private fun ZipOutputStream.putText(
            name: String,
            text: String,
        ) {
            putNextEntry(ZipEntry(name))
            write(text.toByteArray(Charsets.UTF_8))
            closeEntry()
        }
    }
