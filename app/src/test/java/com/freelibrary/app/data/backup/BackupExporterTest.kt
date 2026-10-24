package com.freelibrary.app.data.backup

import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookmark
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.data.local.entity.Highlight
import com.freelibrary.app.data.local.entity.ReadingProgress
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.profile.AvatarDecoder
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.data.repository.InMemoryPreferences
import com.freelibrary.app.data.repository.ProfileRepository
import com.freelibrary.app.data.repository.SettingsRepository
import com.freelibrary.app.domain.model.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
class BackupExporterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var exporter: BackupExporter
    private lateinit var profileRepository: ProfileRepository
    private lateinit var avatarStore: AvatarStore
    private lateinit var profileStore: ProfileBackupStore
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            settings = SettingsRepository(InMemoryPreferences())
            profileRepository = ProfileRepository(InMemoryPreferences())
            avatarStore = AvatarStore(File(temporaryFolder.root, "profile"), AvatarDecoder { _, _ -> null })
            profileStore = ProfileBackupStore(profileRepository, avatarStore, TimeProvider { 1_234L })
            exporter = BackupExporter(database.backupDao(), settings, TimeProvider { 1_234L }, profileStore)
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "1342",
                        title = "Pride and Prejudice",
                        issuedDate = null,
                        primaryLanguage = "en",
                        locc = null,
                        lastModified = "2026-10-16",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun addReaderData() {
        database.bookShelfStateDao().upsert(BookShelfState(bookId, ShelfState.READING, updatedAt = 10L))
        database.readingProgressDao().upsert(ReadingProgress(bookId, "0.5", lastOpenedAt = 11L))
        database.bookmarkDao().insert(Bookmark(bookId = bookId, position = "0.1", note = "nice", createdAt = 12L))
        database.highlightDao().insert(
            Highlight(bookId = bookId, startPosition = "0.2", endPosition = "0.3", color = null, createdAt = 13L),
        )
    }

    private suspend fun exportToZip(parts: Set<BackupPart>): Pair<ExportSummary, LinkedHashMap<String, ByteArray>> {
        val bytes = ByteArrayOutputStream()
        val summary = exporter.export(parts, bytes)
        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes.toByteArray())).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry -> entries[entry.name] = zip.readBytes() }
        }
        return summary to entries
    }

    private fun LinkedHashMap<String, ByteArray>.text(name: String) = getValue(name).toString(Charsets.UTF_8)

    @Test
    fun `the data part holds the shelves, positions, bookmarks and highlights`() =
        runTest {
            addReaderData()

            val (summary, entries) = exportToZip(setOf(BackupPart.DATA))

            assertEquals(listOf(BackupEntries.MANIFEST, BackupEntries.DATA), entries.keys.toList())
            val data = BackupJson.decodeFromString(DataBackup.serializer(), entries.text(BackupEntries.DATA))
            val book = BookRef("Project Gutenberg", "1342")
            assertEquals(listOf(ShelfStateBackup(book, "READING", 10L)), data.shelfStates)
            assertEquals(listOf(ReadingProgressBackup(book, "0.5", 11L)), data.readingProgress)
            assertEquals(listOf(BookmarkBackup(book, "0.1", "nice", 12L)), data.bookmarks)
            assertEquals(listOf(HighlightBackup(book, "0.2", "0.3", null, 13L)), data.highlights)
            assertEquals(4, summary.dataItems)
        }

    @Test
    fun `the manifest names the format, the time and the chosen parts only`() =
        runTest {
            val (_, entries) = exportToZip(setOf(BackupPart.SETTINGS, BackupPart.DATA))

            val manifest = BackupJson.decodeFromString(BackupManifest.serializer(), entries.text(BackupEntries.MANIFEST))
            assertEquals(BACKUP_FORMAT, manifest.format)
            assertEquals(BACKUP_VERSION, manifest.version)
            assertEquals(1_234L, manifest.createdAt)
            assertEquals(listOf(BackupPart.DATA, BackupPart.SETTINGS), manifest.parts)
        }

    @Test
    fun `the settings part holds the current settings`() =
        runTest {
            settings.setThemeMode(ThemeMode.LIGHT)
            settings.setWifiOnlySync(true)

            val (_, entries) = exportToZip(setOf(BackupPart.SETTINGS))

            assertEquals(listOf(BackupEntries.MANIFEST, BackupEntries.SETTINGS), entries.keys.toList())
            assertEquals(
                SettingsBackup("LIGHT", wifiOnlySync = true),
                BackupJson.decodeFromString(SettingsBackup.serializer(), entries.text(BackupEntries.SETTINGS)),
            )
        }

    private fun jpegBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, out)
        return out.toByteArray()
    }

    @Test
    fun `the settings part also holds the name, birthday and picture`() =
        runTest {
            profileRepository.saveDetails("Arash", 500L)
            val picture = jpegBytes()
            assertTrue(avatarStore.saveFinished(ByteArrayInputStream(picture), 1_000_000L))

            val (_, entries) = exportToZip(setOf(BackupPart.SETTINGS))

            assertEquals(listOf(BackupEntries.MANIFEST, BackupEntries.SETTINGS, BackupEntries.AVATAR), entries.keys.toList())
            val saved = BackupJson.decodeFromString(SettingsBackup.serializer(), entries.text(BackupEntries.SETTINGS))
            assertEquals(ProfileBackup("Arash", 500L, hasPicture = true), saved.profile)
            assertArrayEquals(avatarStore.file()!!.readBytes(), entries.getValue(BackupEntries.AVATAR))
        }

    @Test
    fun `a name alone is exported without a picture entry`() =
        runTest {
            profileRepository.saveDetails("Arash", null)

            val (_, entries) = exportToZip(setOf(BackupPart.SETTINGS))

            assertEquals(listOf(BackupEntries.MANIFEST, BackupEntries.SETTINGS), entries.keys.toList())
            val saved = BackupJson.decodeFromString(SettingsBackup.serializer(), entries.text(BackupEntries.SETTINGS))
            assertEquals(ProfileBackup("Arash", null, hasPicture = false), saved.profile)
        }

    @Test
    fun `the data part never carries the profile`() =
        runTest {
            profileRepository.saveDetails("Arash", 500L)

            val (_, entries) = exportToZip(setOf(BackupPart.DATA))

            assertEquals(listOf(BackupEntries.MANIFEST, BackupEntries.DATA), entries.keys.toList())
        }

    @Test
    fun `the books part copies the downloaded files under numbered names`() =
        runTest {
            val file = temporaryFolder.newFile("pg1342.epub").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            database.downloadedBookDao().insert(
                DownloadedBook(bookId = bookId, localFilePath = file.path, format = "epub", downloadedAt = 77L),
            )

            val (summary, entries) = exportToZip(setOf(BackupPart.BOOKS))

            val books = BackupJson.decodeFromString(BooksBackup.serializer(), entries.text(BackupEntries.BOOKS))
            assertEquals(
                listOf(BookFileBackup(BookRef("Project Gutenberg", "1342"), "epub", "books/1", 77L)),
                books.files,
            )
            assertArrayEquals(byteArrayOf(1, 2, 3, 4), entries.getValue("books/1"))
            assertEquals(1, summary.bookFiles)
            assertEquals(0, summary.missingBookFiles)
        }

    @Test
    fun `a download whose file is gone is left out and counted`() =
        runTest {
            database.downloadedBookDao().insert(
                DownloadedBook(
                    bookId = bookId,
                    localFilePath = temporaryFolder.root.resolve("gone.epub").path,
                    format = "epub",
                    downloadedAt = 1L,
                ),
            )

            val (summary, entries) = exportToZip(setOf(BackupPart.BOOKS))

            assertEquals(
                emptyList<BookFileBackup>(),
                BackupJson.decodeFromString(BooksBackup.serializer(), entries.text(BackupEntries.BOOKS)).files,
            )
            assertEquals(0, summary.bookFiles)
            assertEquals(1, summary.missingBookFiles)
        }

    @Test
    fun `the manifest comes first and every entry has a fixed or numbered name`() =
        runTest {
            addReaderData()
            val file = temporaryFolder.newFile("secret-name.epub").apply { writeBytes(byteArrayOf(9)) }
            database.downloadedBookDao().insert(
                DownloadedBook(bookId = bookId, localFilePath = file.path, format = "epub", downloadedAt = 1L),
            )

            val (_, entries) = exportToZip(BackupPart.entries.toSet())

            assertEquals(BackupEntries.MANIFEST, entries.keys.first())
            val allowed = setOf(BackupEntries.MANIFEST, BackupEntries.DATA, BackupEntries.SETTINGS, BackupEntries.BOOKS, "books/1")
            assertEquals(allowed, entries.keys)
            assertFalse(entries.keys.any { it.contains("secret-name") || it.contains("..") })
        }

    @Test
    fun `an export with nothing chosen is refused`() =
        runTest {
            val failure = runCatching { exporter.export(emptySet(), ByteArrayOutputStream()) }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
        }
}
