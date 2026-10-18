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
import kotlinx.coroutines.flow.first
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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class BackupImporterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var exporter: BackupExporter
    private lateinit var importer: BackupImporter
    private lateinit var profileRepository: ProfileRepository
    private lateinit var avatarStore: AvatarStore
    private lateinit var profileStore: ProfileBackupStore
    private lateinit var storageDir: File
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
            storageDir = File(temporaryFolder.root, "books")
            profileRepository = ProfileRepository(InMemoryPreferences())
            avatarStore = AvatarStore(File(temporaryFolder.root, "profile"), AvatarDecoder { _, _ -> null })
            profileStore = ProfileBackupStore(profileRepository, avatarStore, TimeProvider { 1_234L })
            exporter = BackupExporter(database.backupDao(), settings, TimeProvider { 1_234L }, profileStore)
            importer =
                BackupImporter(
                    database.backupDao(),
                    database.bookShelfStateDao(),
                    database.readingProgressDao(),
                    database.bookmarkDao(),
                    database.highlightDao(),
                    database.downloadedBookDao(),
                    settings,
                    BookStorage(storageDir),
                    profileStore,
                )
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

    private suspend fun clearReaderData() {
        database.bookShelfStateDao().deleteAll()
        database.readingProgressDao().deleteAll()
        database.bookmarkDao().deleteAll()
        database.highlightDao().deleteAll()
    }

    private suspend fun exported(parts: Set<BackupPart>): ByteArray {
        val out = ByteArrayOutputStream()
        exporter.export(parts, out)
        return out.toByteArray()
    }

    private suspend fun restore(
        bytes: ByteArray,
        parts: Set<BackupPart>,
    ) = importer.import(ByteArrayInputStream(bytes), parts)

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private fun manifestBytes(
        parts: List<BackupPart>,
        version: Int = BACKUP_VERSION,
        format: String = BACKUP_FORMAT,
    ) = BackupJson.encodeToString(BackupManifest.serializer(), BackupManifest(format, version, 1L, parts)).toByteArray()

    private suspend fun assertRejected(bytes: ByteArray) {
        val failure = runCatching { importer.inspect(ByteArrayInputStream(bytes)) }.exceptionOrNull()
        assertTrue("Expected a BackupFormatException but got $failure", failure is BackupFormatException)
    }

    @Test
    fun `inspect reports when the backup was made and which parts it holds`() =
        runTest {
            val info = importer.inspect(ByteArrayInputStream(exported(setOf(BackupPart.DATA, BackupPart.SETTINGS))))

            assertEquals(BackupInfo(1_234L, listOf(BackupPart.DATA, BackupPart.SETTINGS)), info)
        }

    @Test
    fun `files that are not a backup are refused`() =
        runTest {
            assertRejected(byteArrayOf(1, 2, 3, 4))
            assertRejected(zipOf("hello.txt" to "hi".toByteArray()))
            assertRejected(zipOf(BackupEntries.MANIFEST to manifestBytes(listOf(BackupPart.DATA), format = "something-else")))
            assertRejected(zipOf(BackupEntries.MANIFEST to "not json".toByteArray()))
        }

    @Test
    fun `a backup from a newer app is refused`() =
        runTest {
            assertRejected(zipOf(BackupEntries.MANIFEST to manifestBytes(listOf(BackupPart.DATA), version = BACKUP_VERSION + 1)))
        }

    @Test
    fun `the data comes back after a round trip`() =
        runTest {
            addReaderData()
            val bytes = exported(setOf(BackupPart.DATA))
            clearReaderData()

            val summary = restore(bytes, setOf(BackupPart.DATA))

            assertEquals(4, summary.restoredItems)
            assertEquals(0, summary.skippedItems)
            val dao = database.backupDao()
            assertEquals(listOf("READING"), dao.shelfStates().map { it.state })
            assertEquals(listOf("0.5"), dao.readingProgress().map { it.lastPosition })
            assertEquals(listOf("0.1"), dao.bookmarks().map { it.position })
            assertEquals(listOf("0.2"), dao.highlights().map { it.startPosition })
        }

    @Test
    fun `importing twice does not duplicate anything`() =
        runTest {
            addReaderData()
            val bytes = exported(setOf(BackupPart.DATA))

            val summary = restore(bytes, setOf(BackupPart.DATA))

            assertEquals(0, summary.restoredItems)
            assertEquals(4, summary.skippedItems)
            assertEquals(1, database.backupDao().bookmarks().size)
            assertEquals(1, database.backupDao().highlights().size)
        }

    @Test
    fun `a newer shelf state on the phone is kept and an older one is replaced`() =
        runTest {
            database.bookShelfStateDao().upsert(BookShelfState(bookId, ShelfState.READING, updatedAt = 10L))
            val bytes = exported(setOf(BackupPart.DATA))

            database.bookShelfStateDao().upsert(BookShelfState(bookId, ShelfState.FINISHED, updatedAt = 100L, finishedAt = 100L))
            val kept = restore(bytes, setOf(BackupPart.DATA))
            assertEquals(0, kept.restoredItems)
            assertEquals("FINISHED", database.bookShelfStateDao().getForBook(bookId)!!.state.name)

            database.bookShelfStateDao().upsert(BookShelfState(bookId, ShelfState.WANT_TO_READ, updatedAt = 5L))
            val replaced = restore(bytes, setOf(BackupPart.DATA))
            assertEquals(1, replaced.restoredItems)
            assertEquals("READING", database.bookShelfStateDao().getForBook(bookId)!!.state.name)
        }

    @Test
    fun `items for books this phone does not have are skipped and counted`() =
        runTest {
            addReaderData()
            val bytes = exported(setOf(BackupPart.DATA))
            database.openHelper.writableDatabase.execSQL("DELETE FROM books")

            val summary = restore(bytes, setOf(BackupPart.DATA))

            assertEquals(0, summary.restoredItems)
            assertEquals(4, summary.skippedItems)
        }

    @Test
    fun `the settings come back after a round trip`() =
        runTest {
            settings.setThemeMode(ThemeMode.DARK)
            settings.setTrueBlack(true)
            settings.setWifiOnlySync(false)
            val bytes = exported(setOf(BackupPart.SETTINGS))
            settings.setThemeMode(ThemeMode.LIGHT)
            settings.setTrueBlack(false)
            settings.setWifiOnlySync(true)

            val summary = restore(bytes, setOf(BackupPart.SETTINGS))

            assertTrue(summary.settingsRestored)
            assertEquals(ThemeMode.DARK, settings.themeMode.first())
            assertTrue(settings.trueBlack.first())
            assertFalse(settings.wifiOnlySync.first())
        }

    @Test
    fun `only the parts the reader chooses are restored`() =
        runTest {
            addReaderData()
            settings.setThemeMode(ThemeMode.DARK)
            val bytes = exported(BackupPart.entries.toSet())
            clearReaderData()
            settings.setThemeMode(ThemeMode.LIGHT)

            val summary = restore(bytes, setOf(BackupPart.SETTINGS))

            assertTrue(summary.settingsRestored)
            assertEquals(0, summary.restoredItems)
            assertEquals(ThemeMode.DARK, settings.themeMode.first())
            assertTrue(database.backupDao().shelfStates().isEmpty())
        }

    @Test
    fun `a part the file does not hold is ignored`() =
        runTest {
            val bytes = exported(setOf(BackupPart.SETTINGS))

            val summary = restore(bytes, setOf(BackupPart.DATA))

            assertEquals(0, summary.restoredItems)
            assertFalse(summary.settingsRestored)
        }

    @Test
    fun `book files come back into the books folder and are not restored twice`() =
        runTest {
            val original = temporaryFolder.newFile("pg1342.epub").apply { writeBytes(byteArrayOf(5, 6, 7)) }
            database.downloadedBookDao().insert(
                DownloadedBook(bookId = bookId, localFilePath = original.path, format = "epub", downloadedAt = 77L),
            )
            val bytes = exported(setOf(BackupPart.BOOKS))
            original.delete()
            database.downloadedBookDao().deleteAllFormatsForBook(bookId)

            val first = restore(bytes, setOf(BackupPart.BOOKS))

            assertEquals(1, first.bookFilesRestored)
            val row = database.downloadedBookDao().getFormatsForBook(bookId).single()
            assertEquals(File(storageDir, "$bookId.epub").path, row.localFilePath)
            assertEquals("epub", row.format)
            assertEquals(77L, row.downloadedAt)
            assertArrayEquals(byteArrayOf(5, 6, 7), File(row.localFilePath).readBytes())

            val second = restore(bytes, setOf(BackupPart.BOOKS))
            assertEquals(0, second.bookFilesRestored)
            assertEquals(1, second.bookFilesSkipped)
        }

    @Test
    fun `a crafted entry name cannot write outside the books folder`() =
        runTest {
            val book = BookRef("Project Gutenberg", "1342")
            val list = BooksBackup(listOf(BookFileBackup(book, "../../evil", "books/../../evil", 1L)))
            val bytes =
                zipOf(
                    BackupEntries.MANIFEST to manifestBytes(listOf(BackupPart.BOOKS)),
                    BackupEntries.BOOKS to BackupJson.encodeToString(BooksBackup.serializer(), list).toByteArray(),
                    "books/../../evil" to byteArrayOf(1),
                    "../evil" to byteArrayOf(2),
                )

            val summary = restore(bytes, setOf(BackupPart.BOOKS))

            assertEquals(1, summary.bookFilesRestored)
            assertEquals(listOf("$bookId.evil"), storageDir.listFiles()!!.map { it.name })
            assertFalse(File(temporaryFolder.root, "evil").exists())
            assertFalse(File(temporaryFolder.root.parentFile, "evil").exists())
        }

    private fun jpegBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, out)
        return out.toByteArray()
    }

    private fun settingsJson(profile: String) = """{"themeMode":"DARK","trueBlack":true,"wifiOnlySync":false$profile}""".toByteArray()

    @Test
    fun `the name, birthday and picture come back after a round trip`() =
        runTest {
            profileRepository.saveDetails("Arash", 500L)
            assertTrue(avatarStore.saveFinished(ByteArrayInputStream(jpegBytes()), 1_000_000L))
            val bytes = exported(setOf(BackupPart.SETTINGS))
            profileRepository.saveDetails(null, null)
            avatarStore.remove()
            profileRepository.setAvatarVersion(null)

            val summary = restore(bytes, setOf(BackupPart.SETTINGS))

            assertTrue(summary.profileRestored)
            val profile = profileRepository.profile.first()
            assertEquals("Arash", profile.name)
            assertEquals(500L, profile.birthdayUtcMillis)
            assertEquals(1_234L, profile.avatarVersion)
            assertTrue(avatarStore.file() != null)
        }

    @Test
    fun `a backup without a profile leaves the phone's profile alone`() =
        runTest {
            profileRepository.saveDetails("Sam", 400L)
            val bytes =
                zipOf(
                    BackupEntries.MANIFEST to manifestBytes(listOf(BackupPart.SETTINGS)),
                    BackupEntries.SETTINGS to settingsJson(""),
                )

            val summary = restore(bytes, setOf(BackupPart.SETTINGS))

            assertTrue(summary.settingsRestored)
            assertFalse(summary.profileRestored)
            val profile = profileRepository.profile.first()
            assertEquals("Sam", profile.name)
            assertEquals(400L, profile.birthdayUtcMillis)
        }

    @Test
    fun `a picture entry that is not an image is not kept`() =
        runTest {
            val bytes =
                zipOf(
                    BackupEntries.MANIFEST to manifestBytes(listOf(BackupPart.SETTINGS)),
                    BackupEntries.SETTINGS to settingsJson(""","profile":{"hasPicture":true}"""),
                    BackupEntries.AVATAR to "this is not a picture".toByteArray(),
                )

            val summary = restore(bytes, setOf(BackupPart.SETTINGS))

            assertFalse(summary.profileRestored)
            assertEquals(null, avatarStore.file())
            assertEquals(null, profileRepository.profile.first().avatarVersion)
        }

    @Test
    fun `a bad picture does not replace the phone's own picture`() =
        runTest {
            val own = jpegBytes()
            assertTrue(avatarStore.saveFinished(ByteArrayInputStream(own), 1_000_000L))
            val bytes =
                zipOf(
                    BackupEntries.MANIFEST to manifestBytes(listOf(BackupPart.SETTINGS)),
                    BackupEntries.SETTINGS to settingsJson(""","profile":{"hasPicture":true}"""),
                    BackupEntries.AVATAR to "this is not a picture".toByteArray(),
                )

            restore(bytes, setOf(BackupPart.SETTINGS))

            assertArrayEquals(own, avatarStore.file()!!.readBytes())
        }

    @Test
    fun `a picture over the size limit is not kept`() =
        runTest {
            val big = jpegBytes() + ByteArray(3 * 1024 * 1024)
            val bytes =
                zipOf(
                    BackupEntries.MANIFEST to manifestBytes(listOf(BackupPart.SETTINGS)),
                    BackupEntries.SETTINGS to settingsJson(""","profile":{"hasPicture":true}"""),
                    BackupEntries.AVATAR to big,
                )

            restore(bytes, setOf(BackupPart.SETTINGS))

            assertEquals(null, avatarStore.file())
        }

    @Test
    fun `a birthday in the future is ignored but the name is kept`() =
        runTest {
            val bytes =
                zipOf(
                    BackupEntries.MANIFEST to manifestBytes(listOf(BackupPart.SETTINGS)),
                    BackupEntries.SETTINGS to settingsJson(""","profile":{"name":"Arash","birthdayUtcMillis":999999999}"""),
                )

            restore(bytes, setOf(BackupPart.SETTINGS))

            val profile = profileRepository.profile.first()
            assertEquals("Arash", profile.name)
            assertEquals(null, profile.birthdayUtcMillis)
        }

    @Test
    fun `the profile is not restored when settings are not chosen`() =
        runTest {
            profileRepository.saveDetails("Arash", 500L)
            assertTrue(avatarStore.saveFinished(ByteArrayInputStream(jpegBytes()), 1_000_000L))
            val bytes = exported(BackupPart.entries.toSet())
            profileRepository.saveDetails(null, null)
            avatarStore.remove()

            restore(bytes, setOf(BackupPart.DATA))

            assertEquals(null, profileRepository.profile.first().name)
            assertEquals(null, avatarStore.file())
        }

    @Test
    fun `an import with nothing chosen is refused`() =
        runTest {
            val failure = runCatching { restore(exported(setOf(BackupPart.DATA)), emptySet()) }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
        }
}
