package com.freelibrary.app.ui.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.backup.BackupExporter
import com.freelibrary.app.data.backup.BackupImporter
import com.freelibrary.app.data.backup.BackupPart
import com.freelibrary.app.data.backup.BookStorage
import com.freelibrary.app.data.backup.ProfileBackupStore
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.profile.AvatarDecoder
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.data.repository.InMemoryPreferences
import com.freelibrary.app.data.repository.ProfileRepository
import com.freelibrary.app.data.repository.SettingsRepository
import com.freelibrary.app.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackupViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var viewModel: BackupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FreeLibraryDatabase::class.java,
            ).allowMainThreadQueries().build()
        settings = SettingsRepository(InMemoryPreferences())
        val profileStore =
            ProfileBackupStore(
                ProfileRepository(InMemoryPreferences()),
                AvatarStore(File(temporaryFolder.root, "profile"), AvatarDecoder { _, _ -> null }),
                TimeProvider { 1_234L },
            )
        viewModel =
            BackupViewModel(
                BackupExporter(database.backupDao(), settings, TimeProvider { 1_234L }, profileStore),
                BackupImporter(
                    database.backupDao(),
                    database.bookShelfStateDao(),
                    database.readingProgressDao(),
                    database.bookmarkDao(),
                    database.highlightDao(),
                    database.downloadedBookDao(),
                    settings,
                    BookStorage(File(temporaryFolder.root, "books")),
                    profileStore,
                ),
            )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun finished(): BackupStatus = viewModel.status.first { it !is BackupStatus.Idle && it !is BackupStatus.Working }

    private suspend fun exportSettings(): ByteArray {
        val out = ByteArrayOutputStream()
        viewModel.export(setOf(BackupPart.SETTINGS)) { out }
        assertTrue(finished() is BackupStatus.ExportDone)
        viewModel.resultShown()
        return out.toByteArray()
    }

    @Test
    fun `a new view model is idle`() {
        assertEquals(BackupStatus.Idle, viewModel.status.value)
    }

    @Test
    fun `export reports what it wrote`() =
        runTest {
            val out = ByteArrayOutputStream()

            viewModel.export(setOf(BackupPart.SETTINGS)) { out }

            val done = finished() as BackupStatus.ExportDone
            assertEquals(setOf(BackupPart.SETTINGS), done.summary.parts)
            assertTrue(out.size() > 0)
        }

    @Test
    fun `export fails cleanly when the place cannot be opened`() =
        runTest {
            viewModel.export(setOf(BackupPart.DATA)) { null }

            assertEquals(BackupStatus.Failed(BackupFailure.CANNOT_OPEN), finished())
        }

    @Test
    fun `inspect reports a file that is not a backup`() =
        runTest {
            viewModel.inspect { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }

            assertEquals(BackupStatus.Failed(BackupFailure.INVALID_FILE), finished())
        }

    @Test
    fun `inspect then import restores the chosen part`() =
        runTest {
            settings.setThemeMode(ThemeMode.DARK)
            val bytes = exportSettings()
            settings.setThemeMode(ThemeMode.LIGHT)

            viewModel.inspect { ByteArrayInputStream(bytes) }
            val inspected = finished() as BackupStatus.Inspected
            assertEquals(listOf(BackupPart.SETTINGS), inspected.info.parts)
            viewModel.resultShown()

            viewModel.import(setOf(BackupPart.SETTINGS)) { ByteArrayInputStream(bytes) }
            val done = finished() as BackupStatus.ImportDone
            assertTrue(done.summary.settingsRestored)
            assertEquals(ThemeMode.DARK, settings.themeMode.first())
        }

    @Test
    fun `resultShown returns to idle`() =
        runTest {
            viewModel.export(setOf(BackupPart.SETTINGS)) { null }
            finished()

            viewModel.resultShown()

            assertEquals(BackupStatus.Idle, viewModel.status.value)
        }
}
