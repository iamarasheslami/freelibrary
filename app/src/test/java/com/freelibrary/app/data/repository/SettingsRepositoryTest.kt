package com.freelibrary.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.freelibrary.app.data.reader.ReaderStyle
import com.freelibrary.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The rules are tested against an in-memory DataStore. The real file-backed DataStore replaces
 * its file by renaming a temporary file over it, which the JVM cannot do on Windows (it works on
 * Android and on the Linux CI), so only a single-write smoke test uses a real file.
 */
class SettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `sync is allowed on any connection until the reader turns Wi-Fi only on`() =
        runTest {
            val repository = SettingsRepository(InMemoryPreferences())

            assertFalse(repository.wifiOnlySync.first())

            repository.setWifiOnlySync(true)
            assertTrue(repository.wifiOnlySync.first())

            repository.setWifiOnlySync(false)
            assertFalse(repository.wifiOnlySync.first())
        }

    @Test
    fun `the last sync time is empty until a sync is recorded and then keeps the latest`() =
        runTest {
            val repository = SettingsRepository(InMemoryPreferences())

            assertNull(repository.lastCatalogSyncAt.first())

            repository.recordCatalogSync(1_000L)
            repository.recordCatalogSync(2_000L)

            assertEquals(2_000L, repository.lastCatalogSyncAt.first())
        }

    @Test
    fun `the reader text size is the books own until changed and stays within its limits`() =
        runTest {
            val repository = SettingsRepository(InMemoryPreferences())

            assertEquals(ReaderStyle.DEFAULT_TEXT_PERCENT, repository.readerTextPercent.first())

            repository.setReaderTextPercent(130)
            assertEquals(130, repository.readerTextPercent.first())

            repository.setReaderTextPercent(9_999)
            assertEquals(ReaderStyle.MAX_TEXT_PERCENT, repository.readerTextPercent.first())

            repository.setReaderTextPercent(1)
            assertEquals(ReaderStyle.MIN_TEXT_PERCENT, repository.readerTextPercent.first())
        }

    @Test
    fun `the reader pages by default and remembers a different choice`() =
        runTest {
            val repository = SettingsRepository(InMemoryPreferences())

            assertTrue(repository.readerPaged.first())

            repository.setReaderPaged(false)
            assertFalse(repository.readerPaged.first())

            repository.setReaderPaged(true)
            assertTrue(repository.readerPaged.first())
        }

    @Test
    fun `the theme is Dark until the reader picks one`() =
        runTest {
            val repository = SettingsRepository(InMemoryPreferences())

            assertEquals(ThemeMode.DARK, repository.themeMode.first())
        }

    @Test
    fun `every theme can be saved and read back`() =
        runTest {
            val repository = SettingsRepository(InMemoryPreferences())

            ThemeMode.entries.forEach { mode ->
                repository.setThemeMode(mode)

                assertEquals(mode, repository.themeMode.first())
            }
        }

    @Test
    fun `an unknown stored theme falls back to Dark`() =
        runTest {
            val dataStore = InMemoryPreferences()
            dataStore.edit { it[stringPreferencesKey("theme_mode")] = "SEPIA" }

            assertEquals(ThemeMode.DARK, SettingsRepository(dataStore).themeMode.first())
        }

    @Test
    fun `a theme saved to a real DataStore file is read back`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory.create(scope = backgroundScope) {
                    File(temporaryFolder.root, "settings.preferences_pb")
                }
            val repository = SettingsRepository(dataStore)

            repository.setThemeMode(ThemeMode.DARK)

            assertEquals(ThemeMode.DARK, repository.themeMode.first())
        }
}
