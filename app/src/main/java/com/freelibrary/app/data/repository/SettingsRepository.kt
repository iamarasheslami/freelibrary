package com.freelibrary.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.freelibrary.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The reader's app settings, stored locally with Preferences DataStore. The app language is
 * deliberately not stored here: Android's per-app language support keeps that choice itself.
 */
@Singleton
class SettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /** The chosen theme; System until the reader picks one. */
        val themeMode: Flow<ThemeMode> =
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .map { preferences ->
                    val stored = preferences[THEME_MODE_KEY]
                    ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
                }

        suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode.name }
        }

        private companion object {
            val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        }
    }
