package com.freelibrary.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.freelibrary.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun `the theme is System until the reader picks one`() =
        runTest {
            val repository = SettingsRepository(InMemoryPreferences())

            assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
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
    fun `an unknown stored theme falls back to System`() =
        runTest {
            val dataStore = InMemoryPreferences()
            dataStore.edit { it[stringPreferencesKey("theme_mode")] = "SEPIA" }

            assertEquals(ThemeMode.SYSTEM, SettingsRepository(dataStore).themeMode.first())
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
